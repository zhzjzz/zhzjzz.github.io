#!/usr/bin/env python3
import argparse
import math
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple

import pymysql


TOURISM_DESTINATION_TAGS = {
    "attraction", "museum", "zoo", "theme_park", "viewpoint", "gallery", "camp_site"
}
AMENITY_FACILITY_TAGS = {
    "toilets", "cafe", "restaurant", "fast_food", "parking", "hospital", "pharmacy",
    "bank", "atm", "bus_station", "fuel", "post_office", "library", "school", "university"
}


@dataclass
class OsmFeature:
    name: str
    latitude: float
    longitude: float
    tags: Dict[str, str]


def haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    earth_radius_meters = 6_371_000.0
    delta_lat_rad = math.radians(lat2 - lat1)
    delta_lon_rad = math.radians(lon2 - lon1)
    haversine_a = (
        math.sin(delta_lat_rad / 2) ** 2
        + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(delta_lon_rad / 2) ** 2
    )
    return 2 * earth_radius_meters * math.atan2(math.sqrt(haversine_a), math.sqrt(1 - haversine_a))


def classify(tags: Dict[str, str]) -> Optional[str]:
    tourism = (tags.get("tourism") or "").strip().lower()
    amenity = (tags.get("amenity") or "").strip().lower()
    shop = (tags.get("shop") or "").strip().lower()
    if tourism in TOURISM_DESTINATION_TAGS:
        return "destination"
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
    cursor.execute(
        """
        INSERT INTO destination (name, category, description, latitude, longitude, scene_type, heat, rating)
        VALUES (%s, %s, %s, %s, %s, %s, NULL, NULL)
        ON DUPLICATE KEY UPDATE
            category=VALUES(category),
            description=VALUES(description),
            latitude=VALUES(latitude),
            longitude=VALUES(longitude),
            scene_type=VALUES(scene_type)
        """,
        (feature.name, tourism, description, feature.latitude, feature.longitude, scene_type),
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


def nearest_destination_id(destinations: List[Tuple[int, float, float]], lat: float, lon: float, max_link_meters: float) -> Optional[int]:
    best_id = None
    best_distance = None
    for destination_id, d_lat, d_lon in destinations:
        distance = haversine_meters(lat, lon, d_lat, d_lon)
        if best_distance is None or distance < best_distance:
            best_distance = distance
            best_id = destination_id
    if best_distance is None or best_distance > max_link_meters:
        return None
    return best_id


def main() -> None:
    parser = argparse.ArgumentParser(description="将 OSM(.osm XML) 数据导入 MySQL destination/facility 表")
    parser.add_argument("--osm-file", required=True, help="OSM 文件路径（.osm XML）")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--max-link-meters", type=float, default=2000.0, help="设施关联最近目的地的最大距离")
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
        destination_points: List[Tuple[int, float, float]] = []
        with conn.cursor() as cursor:
            for feature in features:
                if classify(feature.tags) == "destination":
                    destination_id = upsert_destination(cursor, feature)
                    destination_points.append((destination_id, feature.latitude, feature.longitude))
                    destination_count += 1

            for feature in features:
                if classify(feature.tags) == "facility":
                    destination_id = nearest_destination_id(
                        destination_points, feature.latitude, feature.longitude, args.max_link_meters
                    )
                    insert_facility(cursor, feature, destination_id)
                    facility_count += 1

        conn.commit()
        print(f"导入完成：destination={destination_count}, facility={facility_count}")
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
