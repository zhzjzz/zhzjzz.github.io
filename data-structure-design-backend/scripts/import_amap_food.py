"""Import restaurant POIs from AMap Web Service into SQLite.

This importer uses the official AMap Place Around Search endpoint instead of
scraping protected consumer web pages. It reads ``AMAP_KEY`` by default and
stores AMap POI cost values as ``food.average_price`` when ``biz_ext.cost`` is
available.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import sqlite3
import sys
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from food_illustrations import make_food_illustration


AMAP_AROUND_URL = "https://restapi.amap.com/v3/place/around"
AMAP_FOOD_TYPE = "050000"


@dataclass(frozen=True)
class Anchor:
    name: str
    lat: float
    lon: float


@dataclass(frozen=True)
class AmapFoodPoi:
    source_id: str
    name: str
    cuisine: str
    store_name: str
    rating: float
    heat: float
    average_price: float
    lat: float
    lon: float
    image_url: str
    destination_id: int | None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import AMap restaurant POIs into tourism_system.gpkg")
    parser.add_argument("--db", default="data-structure-design-backend/data/tourism_system.gpkg")
    parser.add_argument("--key", default=os.getenv("AMAP_KEY") or os.getenv("VITE_AMAP_KEY"))
    parser.add_argument("--city", default=os.getenv("AMAP_FOOD_CITY", "北京"))
    parser.add_argument("--radius", type=int, default=3000)
    parser.add_argument("--limit", type=int, default=300)
    parser.add_argument("--offset", type=int, default=25)
    parser.add_argument("--sleep", type=float, default=0.2)
    parser.add_argument("--replace-amap", action="store_true")
    parser.add_argument("--around-url", default=AMAP_AROUND_URL)
    return parser.parse_args()


def ensure_columns(conn: sqlite3.Connection) -> None:
    columns = {row[1] for row in conn.execute("PRAGMA table_info(food)")}
    migrations = {
        "heat": "ALTER TABLE food ADD COLUMN heat REAL",
        "average_price": "ALTER TABLE food ADD COLUMN average_price REAL",
        "latitude": "ALTER TABLE food ADD COLUMN latitude REAL",
        "longitude": "ALTER TABLE food ADD COLUMN longitude REAL",
        "source_type": "ALTER TABLE food ADD COLUMN source_type TEXT",
        "source_id": "ALTER TABLE food ADD COLUMN source_id TEXT",
        "image_url": "ALTER TABLE food ADD COLUMN image_url TEXT",
    }
    for column, sql in migrations.items():
        if column not in columns:
            conn.execute(sql)


def load_anchors(conn: sqlite3.Connection) -> list[Anchor]:
    rows = conn.execute(
        """
        SELECT name, latitude, longitude
        FROM destination
        WHERE name IS NOT NULL AND latitude IS NOT NULL AND longitude IS NOT NULL
        UNION
        SELECT s.name, nc.latitude, nc.longitude
        FROM spots s
        JOIN (
            SELECT spot_name, AVG(y) AS latitude, AVG(x) AS longitude
            FROM nodes
            WHERE y IS NOT NULL AND x IS NOT NULL
            GROUP BY spot_name
        ) nc ON nc.spot_name = s.name
        WHERE s.name IS NOT NULL AND nc.latitude IS NOT NULL AND nc.longitude IS NOT NULL
        """
    ).fetchall()
    return [Anchor(str(row[0]), float(row[1]), float(row[2])) for row in rows]


def fetch_around(url: str, key: str, anchor: Anchor, city: str, radius: int, offset: int, page: int) -> dict:
    params = {
        "key": key,
        "location": f"{anchor.lon},{anchor.lat}",
        "radius": str(radius),
        "types": AMAP_FOOD_TYPE,
        "city": city,
        "offset": str(min(max(offset, 1), 25)),
        "page": str(page),
        "extensions": "all",
        "sortrule": "distance",
        "output": "JSON",
    }
    request = urllib.request.Request(
        url + "?" + urllib.parse.urlencode(params),
        headers={"User-Agent": "travel-system-course-project/1.0"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.loads(response.read().decode("utf-8"))
    if data.get("status") != "1":
        raise RuntimeError(f"AMap request failed: {data.get('info')} ({data.get('infocode')})")
    return data


def parse_float(value: object) -> float | None:
    if value in (None, "", []):
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def demo_metric(seed_text: str, low: float, high: float, digits: int = 1) -> float:
    digest = hashlib.sha256(seed_text.encode("utf-8")).hexdigest()
    bucket = int(digest[:8], 16) / 0xFFFFFFFF
    return round(low + (high - low) * bucket, digits)


def cuisine_from_type(type_text: str) -> str:
    parts = [part for part in str(type_text or "").split(";") if part]
    detail = parts[-1] if parts else ""
    if "咖啡" in detail:
        return "咖啡"
    if "快餐" in detail:
        return "快餐"
    if "甜品" in detail or "冷饮" in detail:
        return "甜品"
    if "火锅" in detail:
        return "火锅"
    if "清真" in detail:
        return "清真菜"
    if "外国" in detail or "西餐" in detail:
        return "西式简餐"
    if "中餐" in detail:
        return "中餐"
    return detail[:24] or "餐饮"


def fallback_price(cuisine: str, rating: float, source_id: str) -> float:
    base = 58
    if cuisine in {"咖啡", "甜品", "饮品", "烘焙"}:
        base = 36
    elif cuisine in {"快餐", "面食"}:
        base = 30
    elif cuisine in {"火锅", "京菜", "西式简餐"}:
        base = 92
    return round(base + max(0, rating - 4.2) * 18 + demo_metric(source_id, -8, 18, 0))


def nearest_anchor_id(conn: sqlite3.Connection, lat: float, lon: float, max_distance: float) -> int | None:
    rows = conn.execute(
        """
        SELECT s.spot_id, nc.latitude, nc.longitude
        FROM spots s
        JOIN (
            SELECT spot_name, AVG(y) AS latitude, AVG(x) AS longitude
            FROM nodes
            WHERE y IS NOT NULL AND x IS NOT NULL
            GROUP BY spot_name
        ) nc ON nc.spot_name = s.name
        """
    ).fetchall()
    nearest: tuple[float, int] | None = None
    for row in rows:
        distance = haversine_meters(lat, lon, float(row[1]), float(row[2]))
        if nearest is None or distance < nearest[0]:
            nearest = (distance, int(row[0]))
    return nearest[1] if nearest and nearest[0] <= max_distance else None


def haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def parse_pois(conn: sqlite3.Connection, data: dict, limit: int, seen: set[str]) -> list[AmapFoodPoi]:
    pois: list[AmapFoodPoi] = []
    for item in data.get("pois", []):
        source_id = str(item.get("id") or "").strip()
        name = str(item.get("name") or "").strip()
        location = str(item.get("location") or "")
        if not source_id or not name or "," not in location or source_id in seen:
            continue
        seen.add(source_id)
        lon_text, lat_text = location.split(",", 1)
        lat = float(lat_text)
        lon = float(lon_text)
        biz_ext = item.get("biz_ext") or {}
        rating = parse_float(biz_ext.get("rating")) or demo_metric(source_id + name, 3.8, 4.9)
        cuisine = cuisine_from_type(item.get("type"))
        average_price = parse_float(biz_ext.get("cost")) or fallback_price(cuisine, rating, source_id)
        heat = demo_metric(name + source_id, 55, 99, 0)
        destination_id = nearest_anchor_id(conn, lat, lon, 3500)
        pois.append(
            AmapFoodPoi(
                source_id=source_id,
                name=name[:80],
                cuisine=cuisine,
                store_name=name[:80],
                rating=rating,
                heat=heat,
                average_price=average_price,
                lat=lat,
                lon=lon,
                image_url=make_food_illustration(name, cuisine, "amap:" + source_id),
                destination_id=destination_id,
            )
        )
        if len(pois) >= limit:
            break
    return pois


def import_pois(conn: sqlite3.Connection, pois: list[AmapFoodPoi], replace_amap: bool) -> int:
    if replace_amap:
        conn.execute("DELETE FROM food WHERE source_type = 'amap'")
    changed = 0
    for poi in pois:
        exists = conn.execute(
            "SELECT id FROM food WHERE source_type = 'amap' AND source_id = ? LIMIT 1",
            (poi.source_id,),
        ).fetchone()
        values = (
            poi.name,
            poi.cuisine,
            poi.store_name,
            poi.rating,
            poi.heat,
            poi.average_price,
            poi.lat,
            poi.lon,
            "amap",
            poi.source_id,
            poi.image_url,
            poi.destination_id,
        )
        if exists:
            conn.execute(
                """
                UPDATE food
                SET name = ?, cuisine = ?, store_name = ?, rating = ?, heat = ?,
                    average_price = ?, latitude = ?, longitude = ?, source_type = ?,
                    source_id = ?, image_url = ?, destination_id = ?
                WHERE id = ?
                """,
                values + (exists[0],),
            )
        else:
            conn.execute(
                """
                INSERT INTO food (
                    name, cuisine, store_name, rating, heat, average_price,
                    latitude, longitude, source_type, source_id, image_url, destination_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                values,
            )
        changed += 1
    conn.commit()
    return changed


def main() -> int:
    args = parse_args()
    if not args.key:
        print("AMAP_KEY is required. Set AMAP_KEY or pass --key.", file=sys.stderr)
        return 2
    db_path = Path(args.db)
    if not db_path.exists():
        print(f"Database not found: {db_path}", file=sys.stderr)
        return 2

    started_at = time.time()
    seen: set[str] = set()
    imported: list[AmapFoodPoi] = []
    with sqlite3.connect(db_path) as conn:
        ensure_columns(conn)
        anchors = load_anchors(conn)
        for anchor in anchors:
            if len(imported) >= args.limit:
                break
            page = 1
            while len(imported) < args.limit:
                data = fetch_around(args.around_url, args.key, anchor, args.city, args.radius, args.offset, page)
                batch = parse_pois(conn, data, args.limit - len(imported), seen)
                if not batch:
                    break
                imported.extend(batch)
                if len(batch) < min(max(args.offset, 1), 25):
                    break
                page += 1
                time.sleep(args.sleep)
        changed = import_pois(conn, imported, args.replace_amap)
        total = conn.execute("SELECT COUNT(1) FROM food").fetchone()[0]
        amap_total = conn.execute("SELECT COUNT(1) FROM food WHERE source_type = 'amap'").fetchone()[0]

    print(f"Parsed: {len(imported)}")
    print(f"Inserted or updated: {changed}")
    print(f"Food total: {total}")
    print(f"AMap total: {amap_total}")
    print(f"Elapsed: {time.time() - started_at:.1f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
