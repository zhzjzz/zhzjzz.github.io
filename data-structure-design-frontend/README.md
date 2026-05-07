# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about IDE Support for Vue in the [Vue Docs Scaling up Guide](https://vuejs.org/guide/scaling-up/tooling.html#ide-support).

## OSM 导入脚本（Python）

前端目录提供了 `scripts/import_osm_to_mysql.py`，可将 OSM `.osm` 数据导入后端 MySQL 的 `destination`、`facility` 与 `food` 表。

```bash
python scripts/import_osm_to_mysql.py \
  --osm-file /path/to/map.osm \
  --host 127.0.0.1 --port 3306 \
  --user root --password your_password \
  --database travel_system
```

依赖：`pip install pymysql`
