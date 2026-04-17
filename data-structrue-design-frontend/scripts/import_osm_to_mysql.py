#!/usr/bin/env python3
import argparse
import math
import random
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

import pymysql
import osmium

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

class OsmDataHandler(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.destinations: List[OsmFeature] = []
        self.facilities: List[OsmFeature] = []
        self.foods: List[OsmFeature] = []

    def _process_tags(self, tags, lat, lon):
        name = tags.get("name")
        kind = classify(tags)
        if name and kind:
            feature = OsmFeature(name=name.strip(), latitude=lat, longitude=lon, tags=tags)
            if kind == "destination":
                self.destinations.append(feature)
            elif kind == "facility":
                self.facilities.append(feature)
            elif kind == "food":
                self.foods.append(feature)

    def node(self, n):
        tags = {t.k: t.v for t in n.tags}
        self._process_tags(tags, n.location.lat, n.location.lon)

    def way(self, w):
        tags = {t.k: t.v for t in w.tags}
        if classify(tags) and tags.get("name"):
            lats, lons = [], []
            for n in w.nodes:
                try:
                    lats.append(n.location.lat)
                    lons.append(n.location.lon)
                except osmium.InvalidLocationError:
                    pass
            if lats and lons:
                self._process_tags(tags, sum(lats) / len(lats), sum(lons) / len(lons))

class SpatialGrid:
    def __init__(self, cell_size_degrees=0.02):
        self.cell_size = cell_size_degrees
        self.grid = {}
        self.all_points = [] 

    def add_point(self, point_id: int, lat: float, lon: float):
        cell_x = int(lat / self.cell_size)
        cell_y = int(lon / self.cell_size)
        if (cell_x, cell_y) not in self.grid:
            self.grid[(cell_x, cell_y)] = []
        self.grid[(cell_x, cell_y)].append((point_id, lat, lon))
        self.all_points.append((point_id, lat, lon))

    def get_nearest(self, lat: float, lon: float, max_radius: float, strict: bool) -> Optional[int]:
        cell_x = int(lat / self.cell_size)
        cell_y = int(lon / self.cell_size)
        
        best_id = None
        best_distance = float('inf')

        for dx in [-1, 0, 1]:
            for dy in [-1, 0, 1]:
                cell = (cell_x + dx, cell_y + dy)
                if cell in self.grid:
                    for point_id, d_lat, d_lon in self.grid[cell]:
                        distance = haversine_meters(lat, lon, d_lat, d_lon)
                        if distance < best_distance:
                            best_distance = distance
                            best_id = point_id

        if best_id is not None:
            if best_distance > max_radius:
                return None if strict else best_id
            return best_id
            
        if strict:
            return None
            
        for point_id, d_lat, d_lon in self.all_points:
            distance = haversine_meters(lat, lon, d_lat, d_lon)
            if distance < best_distance:
                best_distance = distance
                best_id = point_id
        return best_id

def main() -> None:
    parser = argparse.ArgumentParser(description="将 OSM(支持 .pbf) 数据极速导入 MySQL")
    parser.add_argument("--osm-file", required=True, help="OSM 文件路径（支持 .pbf 等格式）")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", required=True)
    parser.add_argument("--max-link-meters", type=float, default=2000.0, help="关联最近目的地的最大距离")
    parser.add_argument(
        "--strict-link-radius",
        action="store_true",
        help="超出 max-link-meters 时保持 destination_id 为空 (会导致该设施被忽略，因为数据库不接受空id)",
    )
    parser.add_argument("--limit", type=int, default=0, help="仅导入前 N 条（0 表示不限）")
    args = parser.parse_args()

    print("1. 开始解析地图文件...")
    handler = OsmDataHandler()
    handler.apply_file(args.osm_file, locations=True)
    
    if args.limit > 0:
        handler.destinations = handler.destinations[:args.limit]
        handler.facilities = handler.facilities[:args.limit]
        handler.foods = handler.foods[:args.limit]
        
    print(f"解析完成: 找到 {len(handler.destinations)} 个景点候选, {len(handler.facilities)} 个设施候选, {len(handler.foods)} 个餐饮候选")

    conn = pymysql.connect(
        host=args.host, port=args.port, user=args.user, password=args.password,
        database=args.database, charset="utf8mb4", autocommit=False
    )
    
    try:
        with conn.cursor() as cursor:
            # --- 写入 Destination ---
            print("2. 正在批量写入景点 (Destination)...")
            dest_values = []
            for f in handler.destinations:
                tourism = f.tags.get("tourism")
                # 截断以适应 varchar(1000)
                description = (f.tags.get("description") or f.tags.get("name:en") or "")[:1000]
                scene_type = (tourism or f.tags.get("historic") or "")[:255]
                rating = round(random.uniform(RATING_MIN, RATING_MAX), RATING_PRECISION)
                
                dest_values.append((f.name[:255], tourism, description, f.latitude, f.longitude, scene_type, rating))
            
            cursor.executemany("""
                INSERT INTO destination (name, category, description, latitude, longitude, scene_type, rating, heat)
                VALUES (%s, %s, %s, %s, %s, %s, %s, NULL)
                ON DUPLICATE KEY UPDATE 
                    category=VALUES(category), latitude=VALUES(latitude), longitude=VALUES(longitude),
                    rating=IFNULL(rating, VALUES(rating))
            """, dest_values)
            
            # --- 构建空间网格 ---
            cursor.execute("SELECT id, latitude, longitude FROM destination WHERE latitude IS NOT NULL")
            db_destinations = cursor.fetchall()
            
            print("3. 构建空间网格索引以加速查询...")
            grid = SpatialGrid()
            for row in db_destinations:
                grid.add_point(int(row[0]), float(row[1]), float(row[2]))

            # --- 写入 Facility ---
            print("4. 正在关联并批量写入设施 (Facility)...")
            facility_values = []
            for f in handler.facilities:
                dest_id = grid.get_nearest(f.latitude, f.longitude, args.max_link_meters, args.strict_link_radius)
                
                # 如果没有找到对应的目的地，因为新表规定 destination_id 不能为 null，所以必须跳过
                if dest_id is None:
                    continue
                    
                fac_type = (f.tags.get("amenity") or f.tags.get("shop") or "other")[:255]
                facility_values.append((f.name[:255], fac_type, f.latitude, f.longitude, dest_id))
            
            if facility_values:
                cursor.executemany("""
                    INSERT INTO facility (name, facility_type, latitude, longitude, destination_id)
                    VALUES (%s, %s, %s, %s, %s)
                """, facility_values)

            # --- 写入 Food ---
            print("5. 正在关联并批量写入餐饮 (Food)...")
            food_values = []
            seen_foods = set() 
            
            for f in handler.foods:
                dest_id = grid.get_nearest(f.latitude, f.longitude, args.max_link_meters, args.strict_link_radius)
                
                # 同样，destination_id 不能为 null，必须跳过
                if dest_id is None:
                    continue
                    
                # 数据库表限制了 name 和 cuisine 最大 20，store_name 最大 40
                food_name = f.name[:20]
                food_key = (food_name, dest_id)
                
                if food_key not in seen_foods:
                    seen_foods.add(food_key)
                    cuisine = (f.tags.get("cuisine") or "")[:20]
                    store_name = (f.tags.get("brand") or f.tags.get("operator") or f.name or "")[:40]
                    rating = round(random.uniform(RATING_MIN, RATING_MAX), RATING_PRECISION)
                    food_values.append((food_name, cuisine, store_name, rating, dest_id))
            
            if food_values:
                cursor.executemany("""
                    INSERT INTO food (name, cuisine, store_name, rating, destination_id)
                    VALUES (%s, %s, %s, %s, %s)
                """, food_values)

        conn.commit()
        print(f"\n🎉 导入大功告成！")
        print(f"成功插入: {len(dest_values)} 个景点, {len(facility_values)} 个设施, {len(food_values)} 个餐饮")

    except Exception as exc:
        conn.rollback()
        print(f"\n❌ 导入失败，已回滚事务: {exc}")
        raise
    finally:
        conn.close()

if __name__ == "__main__":
    main()