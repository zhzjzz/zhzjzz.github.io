"""Assign deterministic per-food illustrations to existing SQLite food rows."""

from __future__ import annotations

import argparse
import sqlite3
from pathlib import Path

from food_illustrations import make_food_illustration


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Assign image_url illustrations to food rows")
    parser.add_argument(
        "--db",
        default="data-structure-design-backend/data/tourism_system.gpkg",
        help="SQLite/GeoPackage path",
    )
    parser.add_argument("--overwrite", action="store_true", help="Regenerate image_url even when it already exists")
    return parser.parse_args()


def ensure_image_column(conn: sqlite3.Connection) -> None:
    columns = {row[1] for row in conn.execute("PRAGMA table_info(food)")}
    if "image_url" not in columns:
        conn.execute("ALTER TABLE food ADD COLUMN image_url TEXT")


def main() -> int:
    args = parse_args()
    db_path = Path(args.db)
    if not db_path.exists():
        raise SystemExit(f"Database not found: {db_path}")

    with sqlite3.connect(db_path) as conn:
        ensure_image_column(conn)
        rows = conn.execute(
            """
            SELECT id, name, cuisine, image_url
            FROM food
            WHERE name IS NOT NULL
            """
        ).fetchall()
        updated = 0
        for food_id, name, cuisine, image_url in rows:
            if image_url and not args.overwrite:
                continue
            conn.execute(
                "UPDATE food SET image_url = ? WHERE id = ?",
                (make_food_illustration(name, cuisine, food_id), food_id),
            )
            updated += 1
        conn.commit()
        total_with_images = conn.execute(
            "SELECT COUNT(1) FROM food WHERE image_url IS NOT NULL AND image_url != ''"
        ).fetchone()[0]

    print(f"Updated: {updated}")
    print(f"Rows with images: {total_with_images}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
