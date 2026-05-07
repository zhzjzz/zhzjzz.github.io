## 涓€у寲鏃呮父绯荤粺锛圴ue3 + Java17 + Spring Boot + MySQL + Elasticsearch锛?

鏈粨搴撳凡鐢熸垚涓€涓彲杩愯鐨勫墠鍚庣鍩虹鐗堟湰锛岃鐩栦互涓嬫牳蹇冭兘鍔涳細
- 鐩殑鍦?Top-K 鎺ㄨ崘涓庡叧閿瓧妫€绱?
- 璺嚎瑙勫垝锛堟渶鐭窛绂?鏈€鐭椂闂?+ 浜ら€氬伐鍏烽€氳绾︽潫锛?
- 鏃呮父鏃ヨ绠＄悊涓庡叏鏂囨绱紙ES锛?
- 鍗忎綔琛岀▼鍒涘缓涓庢煡璇?
- 缇庨鎺ㄨ崘 Top-K

### 鐩綍缁撴瀯

- `/data-structure-design-frontend`锛歏ue3 鍓嶇锛圴ite锛?
- `/data-structure-design-backend`锛歋pring Boot 鍚庣锛圝ava17锛?

### 鍚庣鍚姩

```bash
cd data-structure-design-backend
mvn spring-boot:run
```

鍙€氳繃鐜鍙橀噺閰嶇疆锛?
- `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` 锛堥粯璁ゅ嵆 MySQL: `jdbc:mysql://localhost:3306/travel_system`锛沗MYSQL_USERNAME` 涓?`MYSQL_PASSWORD` 闇€鏄惧紡璁剧疆锛屽惁鍒欏簲鐢ㄥ惎鍔ㄤ細澶辫触锛?
- `ES_URIS`
- `SERVER_PORT`
- `GRAPHHOPPER_ENABLED`锛堥粯璁?`true`锛?
- `GRAPHHOPPER_OSM_FILE`锛堥粯璁?`data/osm/china-latest.osm.pbf`锛?
- `GRAPHHOPPER_GRAPH_LOCATION`锛堥粯璁?`data/graph-cache`锛?
- `GRAPHHOPPER_PROFILE`锛堥粯璁?`car`锛?
- `GRAPHHOPPER_VEHICLE`锛堥粯璁?`car`锛?
- `ES_REPOSITORIES_ENABLED`锛堥粯璁?`true`锛?

榛樿鍦板潃锛歚http://localhost:8080`

### GraphHopper + OSM 璺敱

1. 涓嬭浇涓浗鍖?OSM 鏁版嵁锛坄.osm.pbf`锛夊苟鏀剧疆鍒?`data-structure-design-backend/data/osm/china-latest.osm.pbf`锛堝彲閫氳繃鐜鍙橀噺瑕嗙洊锛夈€?
2. 棣栨瀵煎叆鍥炬暟鎹€楁椂杈冮暱锛岃纭繚缂撳瓨鐩綍鏈夊啓鏉冮檺銆?
3. 鎺ㄨ崘鐢ㄨ緝澶у爢鍐呭瓨鍚姩鍚庣锛堝叏鍥芥暟鎹級锛?

```bash
java -Xmx6g -Xms6g -jar your-project.jar
```

### 鍓嶇鍚姩

```bash
cd data-structure-design-frontend
npm install
npm run dev
```

榛樿鍦板潃锛歚http://localhost:5173`

濡傞渶淇敼鍚庣鍦板潃锛?
- 鍦ㄥ墠绔幆澧冨彉閲忎腑璁剧疆 `VITE_API_BASE_URL`锛堥粯璁?`/api`锛?
- 寮€鍙戞ā寮忎篃鍙缃?`VITE_DEV_API_TARGET`锛堥粯璁?`http://localhost:8080`锛変互淇敼 Vite `/api` 浠ｇ悊鐩爣
- 璺嚎椤靛湴鍥鹃渶閰嶇疆楂樺痉 Key锛歚VITE_AMAP_KEY`锛堥渶鍦ㄩ珮寰峰紑鏀惧钩鍙?https://lbs.amap.com/ 娉ㄥ唽骞跺垱寤哄簲鐢ㄨ幏鍙栵紝寤鸿鍦ㄦ帶鍒跺彴鎸夊煙鍚嶉檺鍒?Key锛?

鏁版嵁搴撴枃浠舵斁鍦╜data-structure-design-backend/data`鐩綍涓?

### 宸插疄鐜?API锛堢ず渚嬶級

- `GET /api/destinations`
- `GET /api/destinations/top?k=10`
- `GET /api/route?startLat=...&startLon=...&endLat=...&endLon=...`
- `GET /api/diaries`
- `POST /api/diaries`
- `GET /api/diaries/search?keyword=...`
- `GET /api/itineraries`
- `POST /api/itineraries`
- `GET /api/foods/top?k=10`
- `POST /api/search/sync-all`锛堜竴閿皢 destination/diary/facility/food/itinerary/user_account 鍏ㄩ噺鍚屾鍒?ES锛?


