#!/usr/bin/env python3
import argparse
import math
import random
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple

import pymysql


DESTINATION_TOURISM_TAGS = {
    "attraction", "museum", "zoo", "theme_park", "viewpoint", "gallery", "camp_site"
}
AMENITY_FACILITY_TAGS = {
    "toilets", "parking", "hospital", "pharmacy",
    "bank", "atm", "bus_station", "fuel", "post_office", "library", "school", "university"
}
AMENITY_FOOD_TAGS = {"restaurant", "fast_food", "cafe", "food_court", "bar", "pub", "ice_cream"}
SHOP_FOOD_TAGS = {"bakery", "confectionery", "beverages", "tea", "coffee", "deli"}
EARTH_RADIUS_METERS = 6_371_000.0
RATING_MIN = 3.0
RATING_MAX = 4.9
RATING_PRECISION = 1


@dataclass
class OsmFeature:
    name: str
    latitude: float
    longitude: float
    tags: Dict[str, str]


def haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    delta_lat_rad = math.radians(lat2 - lat1)
    delta_lon_rad = math.radians(lon2 - lon1)
    haversine_a = (
        math.sin(delta_lat_rad / 2) ** 2
        + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(delta_lon_rad / 2) ** 2
    )
    return 2 * EARTH_RADIUS_METERS * math.atan2(math.sqrt(haversine_a), math.sqrt(1 - haversine_a))


def classify(tags: Dict[str, str]) -> Optional[str]:
    tourism = (tags.get("tourism") or "").strip().lower()
    amenity = (tags.get("amenity") or "").strip().lower()
    shop = (tags.get("shop") or "").strip().lower()
    if tourism in DESTINATION_TOURISM_TAGS:
        return "destination"
    if amenity in AMENITY_FOOD_TAGS or shop in SHOP_FOOD_TAGS:
        return "food"
    if amenity in AMENITY_FACILITY_TAGS or shop:
        return "facility"
    return None


def center_of_refs(node_store: Dict[str, Tuple[float, float]], refs: Iterable[str]) -> Optional[Tuple[float, float]]:
    coords = [node_store[ref] for ref in refs if ref in node_store]
    if not coords:
        return None
    lat = sum(c[0] for c in coords) / len(coords)
    lon = sum(c[1] for c in coords) / len(coords)
    return lat, lon


def parse_osm_xml(path: str) -> List[OsmFeature]:
    features: List[OsmFeature] = []
    node_store: Dict[str, Tuple[float, float]] = {}
    way_candidates: List[Tuple[List[str], Dict[str, str]]] = []

    context = ET.iterparse(path, events=("start", "end"))
    for event, elem in context:
        if event == "end" and elem.tag == "node":
            node_id = elem.attrib.get("id")
            lat = elem.attrib.get("lat")
            lon = elem.attrib.get("lon")
            if node_id and lat and lon:
                lat_f = float(lat)
                lon_f = float(lon)
                node_store[node_id] = (lat_f, lon_f)
                tags = {child.attrib["k"]: child.attrib["v"] for child in elem if child.tag == "tag" and "k" in child.attrib and "v" in child.attrib}
                name = tags.get("name")
                kind = classify(tags)
                if name and kind:
                    features.append(OsmFeature(name=name.strip(), latitude=lat_f, longitude=lon_f, tags=tags))
            elem.clear()
        elif event == "end" and elem.tag == "way":
            tags = {child.attrib["k"]: child.attrib["v"] for child in elem if child.tag == "tag" and "k" in child.attrib and "v" in child.attrib}
            if tags.get("name") and classify(tags):
                refs = [child.attrib["ref"] for child in elem if child.tag == "nd" and "ref" in child.attrib]
                way_candidates.append((refs, tags))
            elem.clear()

    for refs, tags in way_candidates:
        center = center_of_refs(node_store, refs)
        if not center:
            continue
        features.append(OsmFeature(name=tags["name"].strip(), latitude=center[0], longitude=center[1], tags=tags))
    return features


def upsert_destination(cursor, feature: OsmFeature) -> int:
    tourism = feature.tags.get("tourism")
    description = feature.tags.get("description") or feature.tags.get("name:en")
    scene_type = tourism or feature.tags.get("historic")
    rating = round(random.uniform(RATING_MIN, RATING_MAX), RATING_PRECISION)
    cursor.execute(
        """
        INSERT INTO destination (name, category, description, latitude, longitude, scene_type, rating)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            category=VALUES(category),
            description=VALUES(description),
            latitude=VALUES(latitude),
            longitude=VALUES(longitude),
            scene_type=VALUES(scene_type),
            rating=IFNULL(rating, VALUES(rating))
        """,
        (feature.name, tourism, description, feature.latitude, feature.longitude, scene_type, rating),
    )
    cursor.execute("SELECT id FROM destination WHERE name=%s", (feature.name,))
    row = cursor.fetchone()
    return int(row[0])


def insert_facility(cursor, feature: OsmFeature, destination_id: Optional[int]) -> None:
    facility_type = feature.tags.get("amenity") or feature.tags.get("shop") or "other"
    cursor.execute(
        """
        INSERT INTO facility (name, facility_type, latitude, longitude, destination_id)
        VALUES (%s, %s, %s, %s, %s)
        """,
        (feature.name, facility_type, feature.latitude, feature.longitude, destination_id),
    )


def upsert_food(cursor, feature: OsmFeature, destination_id: Optional[int]) -> bool:
    cuisine = feature.tags.get("cuisine")
    store_name = feature.tags.get("brand") or feature.tags.get("operator") or feature.name
    # 使用 MySQL 的 null-safe 等号 `<=>`，保证 destination_id 为 NULL 时也能正确去重。
    cursor.execute(
        "SELECT id FROM food WHERE name=%s AND (destination_id <=> %s) LIMIT 1",
        (feature.name, destination_id),
    )
    exists = cursor.fetchone()
    if exists:
        return False
    rating = round(random.uniform(RATING_MIN, RATING_MAX), RATING_PRECISION)
    cursor.execute(
        """
        INSERT INTO food (name, cuisine, store_name, heat, rating, destination_id)
        VALUES (%s, %s, %s, NULL, %s, %s)
        """,
        (feature.name, cuisine, store_name, rating, destination_id),
    )
    return True


def nearest_destination_id(
    destinations: List[Tuple[int, float, float]],
    lat: float,
    lon: float,
    max_link_meters: float,
    strict_link_radius: bool = False,
) -> Optional[int]:
    best_id = None
    best_distance = None
    for destination_id, d_lat, d_lon in destinations:
        distance = haversine_meters(lat, lon, d_lat, d_lon)
        if best_distance is None or distance < best_distance:
            best_distance = distance
            best_id = destination_id
    if best_distance is None:
        return None
    if best_distance > max_link_meters:
        # Fallback to nearest destination when threshold is exceeded.
        return None if strict_link_radius else best_id
    return best_id


def load_destination_points(cursor) -> List[Tuple[int, float, float]]:
    cursor.execute("SELECT id, latitude, longitude FROM destination WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    rows = cursor.fetchall()
    return [(int(row[0]), float(row[1]), float(row[2])) for row in rows]


def main() -> None:
    """解析 OSM 文件并导入 destination/facility/food 三张表。"""
    parser = argparse.ArgumentParser(description="将 OSM(.osm XML) 数据导入 MySQL destination/facility/food 表")
    parser.add_argument("--osm-file", required=True, help="OSM 文件路径（.osm XML）")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--max-link-meters", type=float, default=2000.0, help="facility/food 关联最近目的地的最大距离")
    parser.add_argument(
        "--strict-link-radius",
        "--require-link-within-radius",
        dest="strict_link_radius",
        action="store_true",
        help="超出 max-link-meters 时保持 destination_id 为空",
    )
    parser.add_argument("--limit", type=int, default=0, help="仅导入前 N 条（0 表示不限）")
    args = parser.parse_args()

    features = parse_osm_xml(args.osm_file)
    if args.limit > 0:
        features = features[: args.limit]

    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        autocommit=False,
    )
    try:
        destination_count = 0
        facility_count = 0
        food_inserted_count = 0
        with conn.cursor() as cursor:
            for feature in features:
                if classify(feature.tags) == "destination":
                    upsert_destination(cursor, feature)
                    destination_count += 1

            destination_points = load_destination_points(cursor)

            for feature in features:
                if classify(feature.tags) == "facility":
                    destination_id = nearest_destination_id(
                        destination_points, feature.latitude, feature.longitude, args.max_link_meters, args.strict_link_radius
                    )
                    insert_facility(cursor, feature, destination_id)
                    facility_count += 1
                elif classify(feature.tags) == "food":
                    destination_id = nearest_destination_id(
                        destination_points, feature.latitude, feature.longitude, args.max_link_meters, args.strict_link_radius
                    )
                    if upsert_food(cursor, feature, destination_id):
                        food_inserted_count += 1

        conn.commit()
        print(f"导入完成：destination={destination_count}, facility={facility_count}, food={food_inserted_count}")
    except Exception as exc:
        conn.rollback()
        print(f"导入失败，已回滚事务: {exc}")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
