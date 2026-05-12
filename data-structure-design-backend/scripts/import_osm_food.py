"""Import Beijing food POIs from OpenStreetMap Overpass API into SQLite.

The imported POI name, category, source id, and coordinates come from OSM.
Rating and heat are deterministic demo values for the course recommendation
algorithm; they are not third-party platform ratings.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import sqlite3
import sys
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path


OVERPASS_URL = "https://overpass-api.de/api/interpreter"


@dataclass(frozen=True)
class Spot:
    id: int
    name: str
    heat: float
    rating: float
    lat: float
    lon: float


@dataclass(frozen=True)
class FoodPoi:
    name: str
    cuisine: str
    store_name: str
    rating: float
    heat: float
    lat: float
    lon: float
    source_type: str
    source_id: str
    destination_id: int | None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import Beijing OSM food POIs into tourism_system.gpkg")
    parser.add_argument(
        "--db",
        default="data-structure-design-backend/data/tourism_system.gpkg",
        help="SQLite/GeoPackage path",
    )
    parser.add_argument("--limit", type=int, default=500, help="Maximum POIs to import")
    parser.add_argument("--replace-osm", action="store_true", help="Delete existing OSM-imported food rows first")
    parser.add_argument("--overpass-url", default=OVERPASS_URL, help="Overpass interpreter URL")
    return parser.parse_args()


def fetch_overpass(overpass_url: str, limit: int) -> dict:
    query = f"""
    [out:json][timeout:90];
    area["name"="北京市"]["boundary"="administrative"]->.beijing;
    (
      node["amenity"~"^(restaurant|cafe|fast_food|food_court|ice_cream)$"](area.beijing);
      way["amenity"~"^(restaurant|cafe|fast_food|food_court|ice_cream)$"](area.beijing);
      relation["amenity"~"^(restaurant|cafe|fast_food|food_court|ice_cream)$"](area.beijing);
      node["shop"~"^(bakery|tea|beverages|confectionery)$"](area.beijing);
      way["shop"~"^(bakery|tea|beverages|confectionery)$"](area.beijing);
      relation["shop"~"^(bakery|tea|beverages|confectionery)$"](area.beijing);
    );
    out center {max(limit, 1)};
    """
    payload = urllib.parse.urlencode({"data": query}).encode("utf-8")
    request = urllib.request.Request(
        overpass_url,
        data=payload,
        headers={
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "User-Agent": "travel-system-course-project/1.0",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.loads(response.read().decode("utf-8"))


def ensure_columns(conn: sqlite3.Connection) -> None:
    columns = {row[1] for row in conn.execute("PRAGMA table_info(food)")}
    migrations = {
        "heat": "ALTER TABLE food ADD COLUMN heat REAL",
        "latitude": "ALTER TABLE food ADD COLUMN latitude REAL",
        "longitude": "ALTER TABLE food ADD COLUMN longitude REAL",
        "source_type": "ALTER TABLE food ADD COLUMN source_type TEXT",
        "source_id": "ALTER TABLE food ADD COLUMN source_id TEXT",
    }
    for column, sql in migrations.items():
        if column not in columns:
            conn.execute(sql)


def load_spots(conn: sqlite3.Connection) -> list[Spot]:
    rows = conn.execute(
        """
        SELECT s.spot_id,
               s.name,
               COALESCE(s.hotness, 0),
               COALESCE(s.rating, 0),
               nc.latitude,
               nc.longitude
        FROM spots s
        LEFT JOIN (
            SELECT spot_name, AVG(y) AS latitude, AVG(x) AS longitude
            FROM nodes
            WHERE y IS NOT NULL AND x IS NOT NULL
            GROUP BY spot_name
        ) nc ON nc.spot_name = s.name
        WHERE nc.latitude IS NOT NULL AND nc.longitude IS NOT NULL
        """
    ).fetchall()
    return [Spot(int(row[0]), row[1], float(row[2]), float(row[3]), float(row[4]), float(row[5])) for row in rows]


def haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def nearest_spot_id(spots: list[Spot], lat: float, lon: float, max_distance_meters: float = 3500) -> int | None:
    nearest: tuple[float, int] | None = None
    for spot in spots:
        distance = haversine_meters(lat, lon, spot.lat, spot.lon)
        if nearest is None or distance < nearest[0]:
            nearest = (distance, spot.id)
    if nearest is None or nearest[0] > max_distance_meters:
        return None
    return nearest[1]


def cuisine_from_tags(tags: dict) -> str:
    cuisine = clean_text(tags.get("cuisine"))
    if cuisine:
        first = cuisine.replace(";", ",").split(",")[0].strip()
        return cuisine_label(first)
    amenity = tags.get("amenity", "")
    shop = tags.get("shop", "")
    if amenity == "cafe":
        return "咖啡"
    if amenity == "fast_food":
        return "快餐"
    if amenity == "ice_cream":
        return "甜品"
    if amenity == "food_court":
        return "美食广场"
    if shop == "bakery":
        return "烘焙"
    if shop in {"tea", "beverages"}:
        return "饮品"
    if shop == "confectionery":
        return "甜品"
    return "餐饮"


def cuisine_label(value: str) -> str:
    labels = {
        "chinese": "中餐",
        "beijing": "京菜",
        "coffee_shop": "咖啡",
        "coffee": "咖啡",
        "burger": "西式简餐",
        "pizza": "西式简餐",
        "noodle": "面食",
        "noodles": "面食",
        "hotpot": "火锅",
        "japanese": "日料",
        "korean": "韩餐",
        "thai": "东南亚菜",
        "muslim": "清真菜",
        "halal": "清真菜",
        "vegetarian": "素食",
        "dessert": "甜品",
        "ice_cream": "甜品",
        "tea": "饮品",
        "bakery": "烘焙",
    }
    return labels.get(value.lower(), value[:24] if value else "餐饮")


def clean_text(value: object) -> str:
    if value is None:
        return ""
    return str(value).strip()


def demo_metric(seed_text: str, low: float, high: float, digits: int = 1) -> float:
    digest = hashlib.sha256(seed_text.encode("utf-8")).hexdigest()
    bucket = int(digest[:8], 16) / 0xFFFFFFFF
    return round(low + (high - low) * bucket, digits)


def element_location(element: dict) -> tuple[float, float] | None:
    if "lat" in element and "lon" in element:
        return float(element["lat"]), float(element["lon"])
    center = element.get("center")
    if center and "lat" in center and "lon" in center:
        return float(center["lat"]), float(center["lon"])
    return None


def parse_food_pois(data: dict, spots: list[Spot], limit: int) -> list[FoodPoi]:
    pois: list[FoodPoi] = []
    seen_names: set[tuple[str, str]] = set()
    for element in data.get("elements", []):
        tags = element.get("tags") or {}
        name = clean_text(tags.get("name") or tags.get("brand"))
        if not name:
            continue
        location = element_location(element)
        if location is None:
            continue
        lat, lon = location
        source_type = "osm"
        source_id = f"{element.get('type')}:{element.get('id')}"
        dedupe_key = (name, source_id)
        if dedupe_key in seen_names:
            continue
        seen_names.add(dedupe_key)

        cuisine = cuisine_from_tags(tags)
        store_name = clean_text(tags.get("brand")) or name
        rating = demo_metric(source_id + name, 3.8, 4.9)
        heat = demo_metric(name + source_id, 45, 98, 0)
        destination_id = nearest_spot_id(spots, lat, lon)
        pois.append(
            FoodPoi(
                name=name[:80],
                cuisine=cuisine,
                store_name=store_name[:80],
                rating=rating,
                heat=heat,
                lat=lat,
                lon=lon,
                source_type=source_type,
                source_id=source_id,
                destination_id=destination_id,
            )
        )
        if len(pois) >= limit:
            break
    return pois


def import_pois(conn: sqlite3.Connection, pois: list[FoodPoi], replace_osm: bool) -> int:
    if replace_osm:
        conn.execute("DELETE FROM food WHERE source_type = 'osm'")
    inserted = 0
    for poi in pois:
        exists = conn.execute(
            "SELECT 1 FROM food WHERE source_type = ? AND source_id = ? LIMIT 1",
            (poi.source_type, poi.source_id),
        ).fetchone()
        if exists:
            continue
        conn.execute(
            """
            INSERT INTO food (
                name, cuisine, store_name, rating, heat,
                latitude, longitude, source_type, source_id, destination_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                poi.name,
                poi.cuisine,
                poi.store_name,
                poi.rating,
                poi.heat,
                poi.lat,
                poi.lon,
                poi.source_type,
                poi.source_id,
                poi.destination_id,
            ),
        )
        inserted += 1
    conn.commit()
    return inserted


def main() -> int:
    args = parse_args()
    db_path = Path(args.db)
    if not db_path.exists():
        print(f"Database not found: {db_path}", file=sys.stderr)
        return 2

    started_at = time.time()
    print(f"Fetching OSM food POIs from {args.overpass_url} ...")
    data = fetch_overpass(args.overpass_url, args.limit)

    with sqlite3.connect(db_path) as conn:
        ensure_columns(conn)
        spots = load_spots(conn)
        pois = parse_food_pois(data, spots, args.limit)
        inserted = import_pois(conn, pois, args.replace_osm)
        total = conn.execute("SELECT COUNT(1) FROM food").fetchone()[0]
        osm_total = conn.execute("SELECT COUNT(1) FROM food WHERE source_type = 'osm'").fetchone()[0]

    print(f"Parsed: {len(pois)}")
    print(f"Inserted: {inserted}")
    print(f"Food total: {total}")
    print(f"OSM total: {osm_total}")
    print(f"Elapsed: {time.time() - started_at:.1f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
