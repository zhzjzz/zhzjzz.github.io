<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Delete, MapDistance, MapRoad, Plus, PreviewOpen, Ranking, Timer } from '@icon-park/vue-next'
import { getNavNodes, listNavBuildingsBySpot, listNavPoisBySpot, planMultiSpotRoute, searchNavSpots } from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'
import routeDefaultImage from '../assets/defaults/route-default.png'

const amapSecret = (import.meta.env.VITE_AMAP_SECRET || '').trim()
if (amapSecret) {
  window._AMapSecurityConfig = { securityJsCode: amapSecret }
}

const CHINA_CENTER = [104.1954, 35.8617]
const DEFAULT_ZOOM = 4
const AMap_VERSION = '2.0'
const AMap_PLUGINS = ['AMap.Scale', 'AMap.ToolBar', 'AMap.Driving']
const MAP_FIT_PADDING = [70, 70, 70, 70]
const MAP_MAX_ZOOM = 17
const SEGMENT_COLORS = ['#ff385c', '#176b5d', '#f59e0b', '#2563eb', '#7c3aed', '#111827']

const mapInstance = ref(null)
const mapApi = ref(null)
const polylineLayers = ref([])
const drivingLayers = ref([])
const markerOverlays = ref([])
const loading = ref(false)
const demoLoading = ref(false)
const nodeLoading = ref(false)
const routeResult = ref(null)
const strategy = ref('SHORTEST_TIME')
const optimizeVisitOrder = ref(false)
const visits = ref([])
const demoSpotKeywords = ['西湖', '故宫', '公园', '大学']

const newVisit = () => ({
  id: `${Date.now()}-${Math.random()}`,
  spotInput: '',
  spot: null,
  nodes: [],
  places: [],
  nodeIds: [],
  transportMode: 'walk',
})

visits.value = [newVisit(), newVisit()]

const totalDistanceKm = computed(() => ((routeResult.value?.totalDistance || 0) / 1000).toFixed(2))
const totalDurationMin = computed(() => ((routeResult.value?.totalTime || 0) / 60).toFixed(1))

const segmentStats = computed(() => {
  const segments = routeResult.value?.segments || []
  return segments.map((segment, index) => ({
    ...segment,
    index: index + 1,
    distanceKm: ((segment.distance || 0) / 1000).toFixed(2),
    timeMin: ((segment.time || 0) / 60).toFixed(1),
  }))
})

const formatNode = (node) => {
  const floor = node.floor == null ? '' : ` F${node.floor}`
  return `${node.osmid}${floor}`
}

const placeLabel = (place) => `${place.name || place.nearestNodeId} · ${place.type || '地点'}`

const nodeOptions = (visit) => {
  const placeOptions = visit.places
    .filter((place) => place.nearestNodeId != null)
    .map((place) => ({ label: placeLabel(place), value: place.nearestNodeId }))
  const seen = new Set(placeOptions.map((item) => item.value))
  const rawNodeOptions = visit.nodes
    .filter((node) => node.osmid != null && !seen.has(node.osmid))
    .slice(0, 300)
    .map((node) => ({ label: `路网节点 ${formatNode(node)}`, value: node.osmid }))
  return [...placeOptions, ...rawNodeOptions]
}

const clearPolylines = () => {
  if (polylineLayers.value.length && mapInstance.value) {
    mapInstance.value.remove(polylineLayers.value)
  }
  polylineLayers.value = []
  drivingLayers.value.forEach((driving) => driving?.clear?.())
  drivingLayers.value = []
}

const clearMarkers = () => {
  if (markerOverlays.value.length && mapInstance.value) {
    mapInstance.value.remove(markerOverlays.value)
  }
  markerOverlays.value = []
}

const initMap = async () => {
  const amapKey = (import.meta.env.VITE_AMAP_KEY || '').trim()
  if (!amapKey) {
    ElMessage.warning('未配置 VITE_AMAP_KEY，地图无法加载')
    return
  }
  try {
    const AMap = await AMapLoader.load({
      key: amapKey,
      version: AMap_VERSION,
      plugins: AMap_PLUGINS,
    })
    mapApi.value = AMap
    mapInstance.value = new AMap.Map('route-map', {
      zoom: DEFAULT_ZOOM,
      center: CHINA_CENTER,
      mapStyle: 'amap://styles/normal',
      viewMode: '2D',
      features: ['bg', 'road', 'point'],
    })
    mapInstance.value.addControl(new AMap.Scale())
    mapInstance.value.addControl(new AMap.ToolBar({ position: 'RB' }))
  } catch (error) {
    ElMessage.error(`高德地图加载失败：${error?.message || '请检查 Key 或网络'}`)
  }
}

const createMarker = ({ lng, lat, color, title }) => {
  const marker = new mapApi.value.CircleMarker({
    center: [lng, lat],
    radius: 9,
    strokeColor: '#ffffff',
    strokeWeight: 2,
    fillColor: color,
    fillOpacity: 1,
    zIndex: 120,
  })
  const text = new mapApi.value.Text({
    position: [lng, lat],
    text: title,
    anchor: 'bottom-center',
    offset: [0, -16],
    style: {
      background: '#111827',
      color: '#ffffff',
      border: 'none',
      borderRadius: '999px',
      padding: '4px 10px',
      fontSize: '12px',
      fontWeight: 700,
      boxShadow: '0 8px 24px rgba(0,0,0,0.22)',
    },
    zIndex: 130,
  })
  return [marker, text]
}

const drawMicroPath = (coords, color, dashed = false) => {
  if (!mapInstance.value || !mapApi.value || !coords?.length) return null
  const gcjPath = wgs84ToGcj02Batch(coords)
  const amapPath = gcjPath.map(([lat, lng]) => [lng, lat])
  const polyline = new mapApi.value.Polyline({
    path: amapPath,
    strokeColor: color,
    strokeWeight: dashed ? 6 : 7,
    strokeOpacity: 0.94,
    strokeStyle: dashed ? 'dashed' : 'solid',
    isOutline: true,
    outlineColor: '#ffffff',
    lineJoin: 'round',
    showDir: true,
  })
  mapInstance.value.add(polyline)
  return polyline
}

const drawCityLink = (startCoord, endCoord) => {
  if (!startCoord || !endCoord) return null
  const [startLat, startLng] = startCoord
  const [endLat, endLng] = endCoord
  const [gcjStartLng, gcjStartLat] = wgs84ToGcj02(startLng, startLat)
  const [gcjEndLng, gcjEndLat] = wgs84ToGcj02(endLng, endLat)
  const driving = new mapApi.value.Driving({
    map: mapInstance.value,
    hideMarkers: true,
  })
  driving.search(
    new mapApi.value.LngLat(gcjStartLng, gcjStartLat),
    new mapApi.value.LngLat(gcjEndLng, gcjEndLat)
  )
  drivingLayers.value.push(driving)
  return driving
}

const drawSelectedMarkers = () => {
  if (!mapInstance.value || !mapApi.value) return
  clearMarkers()
  visits.value.forEach((visit, visitIndex) => {
    visit.nodeIds.forEach((nodeId, pointIndex) => {
      const node = visit.nodes.find((item) => item.osmid === nodeId)
      if (!node) return
      const [lng, lat] = wgs84ToGcj02(node.x, node.y)
      markerOverlays.value.push(...createMarker({
        lng,
        lat,
        color: SEGMENT_COLORS[visitIndex % SEGMENT_COLORS.length],
        title: `${visitIndex + 1}-${pointIndex + 1} ${visit.spot?.name || ''}`,
      }))
    })
  })
  if (markerOverlays.value.length) {
    mapInstance.value.add(markerOverlays.value)
  }
}

const refreshMapView = () => {
  if (!mapInstance.value) return
  const overlays = [...polylineLayers.value, ...markerOverlays.value].filter(Boolean)
  if (overlays.length) {
    mapInstance.value.setFitView(overlays, false, MAP_FIT_PADDING, MAP_MAX_ZOOM)
  } else {
    mapInstance.value.setZoomAndCenter(DEFAULT_ZOOM, CHINA_CENTER)
  }
}

const fetchSpotSuggestions = async (queryString, callback) => {
  const keyword = (queryString || '').trim()
  if (!keyword) {
    callback([])
    return
  }
  try {
    const { data } = await searchNavSpots({ keyword, limit: 10 })
    callback((data || []).map((item) => ({ value: item.name, raw: item })))
  } catch {
    callback([])
  }
}

const loadVisitData = async (visit) => {
  if (!visit.spot?.name) return
  nodeLoading.value = true
  try {
    const [nodesRes, buildingsRes, poisRes] = await Promise.all([
      getNavNodes(visit.spot.name),
      listNavBuildingsBySpot(visit.spot.name),
      listNavPoisBySpot(visit.spot.name),
    ])
    visit.nodes = Array.isArray(nodesRes.data) ? nodesRes.data : []
    visit.places = [
      ...(Array.isArray(buildingsRes.data) ? buildingsRes.data : []),
      ...(Array.isArray(poisRes.data) ? poisRes.data : []),
    ]
    const defaults = visit.places
      .filter((item) => item.nearestNodeId != null)
      .slice(0, 2)
      .map((item) => item.nearestNodeId)
    visit.nodeIds = defaults.length ? defaults : (visit.nodes[0]?.osmid ? [visit.nodes[0].osmid] : [])
  } finally {
    nodeLoading.value = false
  }
}

const onSpotSelect = async (visit, item) => {
  visit.spot = item.raw
  visit.spotInput = item.value
  visit.nodes = []
  visit.places = []
  visit.nodeIds = []
  await loadVisitData(visit)
  drawSelectedMarkers()
  refreshMapView()
}

const onSpotInput = (visit, value) => {
  if (visit.spot && value !== visit.spot.name) {
    visit.spot = null
    visit.nodes = []
    visit.places = []
    visit.nodeIds = []
    drawSelectedMarkers()
  }
}

const findDemoSpots = async () => {
  const selected = []
  for (const keyword of demoSpotKeywords) {
    const { data } = await searchNavSpots({ keyword, limit: 4 })
    for (const spot of data || []) {
      if (spot?.name && !selected.some((item) => item.name === spot.name)) {
        selected.push(spot)
      }
      if (selected.length >= 2) return selected
    }
  }
  return selected
}

const chooseDemoNodeIds = (visit) => {
  const options = nodeOptions(visit)
  return options.slice(0, 2).map((item) => item.value).filter((value) => value != null)
}

const applyDemoScenario = async () => {
  demoLoading.value = true
  routeResult.value = null
  clearPolylines()
  clearMarkers()
  try {
    const spots = await findDemoSpots()
    if (spots.length < 1) {
      ElMessage.warning('没有找到可用于演示的景区数据，请手动搜索景区')
      return
    }
    visits.value = spots.slice(0, 2).map((spot, index) => ({
      ...newVisit(),
      spotInput: spot.name,
      spot,
      transportMode: index === 0 ? 'walk' : 'bike',
    }))
    for (const visit of visits.value) {
      await loadVisitData(visit)
      visit.nodeIds = chooseDemoNodeIds(visit)
    }
    strategy.value = 'SHORTEST_TIME'
    optimizeVisitOrder.value = true
    drawSelectedMarkers()
    refreshMapView()
    ElMessage.success('演示路线参数已填充，可以直接点击规划路线')
  } catch (error) {
    console.error(error)
    ElMessage.error('演示数据填充失败，请检查后端服务和数据库')
  } finally {
    demoLoading.value = false
  }
}

const addVisit = () => {
  visits.value.push(newVisit())
}

const removeVisit = (index) => {
  if (visits.value.length <= 1) {
    ElMessage.warning('至少保留一个景区')
    return
  }
  visits.value.splice(index, 1)
  drawSelectedMarkers()
  refreshMapView()
}

const moveVisit = (index, direction) => {
  const next = index + direction
  if (next < 0 || next >= visits.value.length) return
  const [item] = visits.value.splice(index, 1)
  visits.value.splice(next, 0, item)
}

const buildPayload = () => ({
  strategy: strategy.value,
  optimizeVisitOrder: optimizeVisitOrder.value,
  visits: visits.value
    .filter((visit) => visit.spot?.name)
    .map((visit) => ({
      spotName: visit.spot.name,
      nodeIds: visit.nodeIds,
      transportMode: visit.transportMode,
    })),
})

const drawRouteResult = (data) => {
  clearPolylines()
  const segments = data?.segments || []
  segments.forEach((segment, index) => {
    const color = SEGMENT_COLORS[index % SEGMENT_COLORS.length]
    if (segment.type === 'city') {
      drawCityLink(segment.cityTransitStart, segment.cityTransitEnd)
      return
    }
    if (segment.path?.length) {
      const line = drawMicroPath(segment.path, color)
      if (line) polylineLayers.value.push(line)
    }
  })
  drawSelectedMarkers()
  refreshMapView()
}

const submit = async () => {
  const payload = buildPayload()
  if (!payload.visits.length) {
    ElMessage.warning('请先添加至少一个景区')
    return
  }
  if (payload.visits.some((visit) => !visit.nodeIds?.length)) {
    ElMessage.warning('每个景区至少选择一个地点')
    return
  }

  loading.value = true
  routeResult.value = null
  try {
    const { data } = await planMultiSpotRoute(payload)
    routeResult.value = data
    drawRouteResult(data)
    ElMessage.success('多景区路线规划完成')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '路线规划失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  await initMap()
  mapInstance.value?.resize()
})

onBeforeUnmount(() => {
  clearPolylines()
  clearMarkers()
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
  mapApi.value = null
})
</script>

<template>
  <section class="route-page">
    <el-card class="module-card route-card">
      <div class="module-header route-hero">
        <div>
          <p class="eyebrow">Multi-scenic routing</p>
          <h2>多景区多地点路线规划</h2>
          <p class="module-subtitle">按访问顺序串联多个景区；同一景区内可选多个地点，并支持步行/电动车、最短时间/最短距离。</p>
        </div>
        <img class="route-hero-image" :src="routeDefaultImage" alt="路线规划默认图" />
        <div class="summary-pill" v-if="routeResult">
          <strong>{{ totalDistanceKm }} km</strong>
          <span>{{ totalDurationMin }} 分钟</span>
        </div>
      </div>

      <div class="planner-toolbar">
        <el-radio-group v-model="strategy">
          <el-radio-button label="SHORTEST_TIME">最优时间</el-radio-button>
          <el-radio-button label="SHORTEST_DISTANCE">最短距离</el-radio-button>
        </el-radio-group>
        <el-switch
          v-model="optimizeVisitOrder"
          active-text="优化景区内地点顺序"
          inactive-text="按选择顺序访问"
        />
        <div class="toolbar-actions">
          <el-button :loading="demoLoading" @click="applyDemoScenario">
            <PreviewOpen theme="outline" size="16" fill="currentColor" />
            演示模式
          </el-button>
          <el-button @click="addVisit">
            <Plus theme="outline" size="16" fill="currentColor" />
            添加景区
          </el-button>
          <el-button type="primary" :loading="loading" @click="submit">
            <MapRoad theme="outline" size="16" fill="currentColor" />
            规划路线
          </el-button>
        </div>
      </div>

      <div class="visit-list">
        <div v-for="(visit, index) in visits" :key="visit.id" class="visit-card">
          <div class="visit-index">{{ index + 1 }}</div>
          <div class="visit-main">
            <el-row :gutter="12">
              <el-col :lg="7" :md="12" :xs="24">
                <label class="field-label">景区/校园</label>
                <el-autocomplete
                  v-model="visit.spotInput"
                  class="full-width"
                  clearable
                  placeholder="搜索景区、校园或公园"
                  :fetch-suggestions="fetchSpotSuggestions"
                  @select="(item) => onSpotSelect(visit, item)"
                  @input="(value) => onSpotInput(visit, value)"
                />
              </el-col>
              <el-col :lg="10" :md="12" :xs="24">
                <label class="field-label">景区内地点，可多选并按选择顺序规划</label>
                <el-select
                  v-model="visit.nodeIds"
                  class="full-width"
                  multiple
                  filterable
                  clearable
                  collapse-tags
                  collapse-tags-tooltip
                  :loading="nodeLoading"
                  placeholder="选择建筑、POI 或路网节点"
                  @change="drawSelectedMarkers"
                >
                  <el-option
                    v-for="item in nodeOptions(visit)"
                    :key="`${visit.id}-${item.value}`"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-col>
              <el-col :lg="4" :md="8" :xs="24">
                <label class="field-label">景区内交通</label>
                <el-select v-model="visit.transportMode" class="full-width">
                  <el-option label="步行" value="walk" />
                  <el-option label="电动车" value="bike" />
                </el-select>
              </el-col>
              <el-col :lg="3" :md="16" :xs="24" class="visit-actions">
                <el-button-group>
                  <el-button @click="moveVisit(index, -1)" :disabled="index === 0">上移</el-button>
                  <el-button @click="moveVisit(index, 1)" :disabled="index === visits.length - 1">下移</el-button>
                </el-button-group>
                <el-button link type="danger" @click="removeVisit(index)">
                  <Delete theme="outline" size="15" fill="currentColor" />
                  删除
                </el-button>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>

      <el-divider />
      <div class="map-title">
        <span>地图路线 · 景区内路径由本系统规划，跨景区城市段使用高德实际道路导航</span>
      </div>
      <div id="route-map" class="route-map" />

      <el-divider />
      <section v-if="routeResult" class="route-summary-grid reveal-in">
        <article class="metric-card">
          <span><MapDistance theme="outline" size="15" fill="currentColor" /> 总距离</span>
          <strong>{{ totalDistanceKm }} km</strong>
        </article>
        <article class="metric-card">
          <span><Timer theme="outline" size="15" fill="currentColor" /> 总耗时</span>
          <strong>{{ totalDurationMin }} 分钟</strong>
        </article>
        <article class="metric-card">
          <span><Ranking theme="outline" size="15" fill="currentColor" /> 路线段数</span>
          <strong>{{ routeResult.segments?.length || 0 }}</strong>
        </article>
      </section>

      <div v-if="routeResult" class="segment-list">
        <div v-for="segment in segmentStats" :key="segment.index" class="segment-item">
          <strong>{{ segment.index }}. {{ segment.fromSpotName }} → {{ segment.toSpotName }}</strong>
          <span>{{ segment.type === 'city' ? (segment.transitType || '城市交通') : (segment.transportMode === 'bike' ? '电动车' : '步行') }}</span>
          <span>{{ segment.distanceKm }} km</span>
          <span>{{ segment.timeMin }} 分钟</span>
        </div>
      </div>
      <el-empty v-else description="添加景区和地点后开始规划" />
    </el-card>
  </section>
</template>

<style scoped>
.full-width {
  width: 100%;
}

.route-hero {
  align-items: flex-start;
  gap: 18px;
}

.route-hero-image {
  width: min(260px, 26vw);
  aspect-ratio: 16 / 10;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.22);
}

.eyebrow {
  margin: 0 0 8px;
  color: #ff385c;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.summary-pill {
  min-width: 150px;
  padding: 14px 18px;
  border-radius: 18px;
  color: #ffffff;
  background: linear-gradient(135deg, #111827, #ff385c);
  box-shadow: 0 18px 36px rgba(17, 24, 39, 0.18);
}

.summary-pill strong,
.summary-pill span {
  display: block;
}

.summary-pill strong {
  font-size: 24px;
}

.planner-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin: 18px 0;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.visit-list {
  display: grid;
  gap: 14px;
}

.visit-card {
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255,255,255,0.96), rgba(255,241,244,0.72));
  box-shadow: 0 18px 42px rgba(17, 24, 39, 0.07);
}

.visit-index {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  color: #ffffff;
  font-size: 18px;
  font-weight: 900;
  background: #111827;
}

.field-label {
  display: block;
  margin: 0 0 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.visit-actions {
  display: flex;
  align-items: end;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.route-map {
  width: 100%;
  height: 560px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(255, 56, 92, 0.18);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.12);
  background: linear-gradient(135deg, #fff8f8 0%, #f8fafc 100%);
}

.map-title {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 7px 13px;
  border-radius: 999px;
  background: #fff1f4;
  color: #ff385c;
  font-size: 13px;
  font-weight: 800;
}

.segment-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.route-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.segment-item {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 110px 90px 90px;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: #f8fafc;
  color: #334155;
}

.segment-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #ff385c, #0f766e);
}

.segment-item strong {
  color: #111827;
}

@media (max-width: 768px) {
  .visit-card {
    grid-template-columns: 1fr;
  }

  .visit-actions {
    justify-content: flex-start;
  }

  .segment-item {
    grid-template-columns: 1fr;
  }

  .route-summary-grid {
    grid-template-columns: 1fr;
  }

  .route-map {
    height: 420px;
  }
}
</style>
