<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Delete, MapDistance, MapRoad, Plus, PreviewOpen, Ranking, Timer } from '@icon-park/vue-next'
import {
  getNavNodes,
  listIndoorBuildings,
  listNavBuildingsBySpot,
  listNavPoisBySpot,
  planIndoorRoute,
  planMultiSpotRoute,
  searchNavSpots,
} from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'
import { isPublicFacilityPlace } from '../utils/placeVisibility'
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
const indoorLoading = ref(false)
const nodeLoading = ref(false)
const routeResult = ref(null)
const indoorResult = ref(null)
const indoorBuildings = ref([])
const indoorExpanded = ref(true)
const selectedIndoorFloor = ref(null)
const strategy = ref('SHORTEST_TIME')
const optimizeVisitOrder = ref(true)
const returnToStart = ref(true)
const visits = ref([])
const indoorForm = ref({
  buildingId: '',
  fromNodeId: '',
  toNodeId: '',
  strategy: 'SHORTEST_DISTANCE',
})
const demoSpotKeywords = ['故宫', '天坛', '公园', '博物馆', '大学']

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
const selectedIndoorBuilding = computed(() => (
  indoorBuildings.value.find((building) => building.id === indoorForm.value.buildingId) || null
))
const indoorPointOptions = computed(() => (
  selectedIndoorBuilding.value?.points?.map((point) => ({
    value: point.id,
    label: `${point.floor}F · ${point.name}`,
  })) || []
))
const indoorDistanceText = computed(() => `${Math.round(indoorResult.value?.totalDistance || 0)} m`)
const indoorPointMap = computed(() => new Map(
  (selectedIndoorBuilding.value?.points || []).map((point) => [point.id, point])
))
const indoorRouteKey = (fromNodeId, toNodeId) => [fromNodeId, toNodeId].sort().join('__')
const indoorRouteConnectionKeys = computed(() => new Set(
  (indoorResult.value?.steps || []).map((step) => indoorRouteKey(step.fromNodeId, step.toNodeId))
))
const indoorRouteNodeIds = computed(() => new Set(
  (indoorResult.value?.steps || []).flatMap((step) => [step.fromNodeId, step.toNodeId])
))
const indoorFloorPoints = (floor) => (
  selectedIndoorBuilding.value?.points?.filter((point) => point.floor === floor) || []
)
const indoorFloorConnections = (floor) => (
  selectedIndoorBuilding.value?.connections?.filter((connection) => {
    const from = indoorPointMap.value.get(connection.fromNodeId)
    const to = indoorPointMap.value.get(connection.toNodeId)
    return from?.floor === floor && to?.floor === floor
  }) || []
)
const indoorPointX = (nodeId) => indoorPointMap.value.get(nodeId)?.x ?? 0
const indoorPointY = (nodeId) => indoorPointMap.value.get(nodeId)?.y ?? 0
const indoorLabelWidth = (name = '') => Math.min(23, Math.max(10, name.length * 2.25 + 4))
const indoorLabelX = (point) => {
  const half = indoorLabelWidth(point.name) / 2
  const offset = point.x <= 28 ? 12 : point.x >= 66 ? 12 : 0
  const centerOffset = offset || (point.y <= 50 ? 0 : point.x < 50 ? -10 : 10)
  return Math.min(100 - half - 3, Math.max(half + 3, point.x + centerOffset))
}
const indoorLabelY = (point) => {
  if (point.y > 75) return Math.min(94, point.y + 10)
  if (point.y < 26) return point.y + 12
  if (point.x >= 66) return point.y + (point.y < 50 ? -9 : 9)
  if (point.x <= 28) return point.y + (point.y < 50 ? -9 : 9)
  return point.y <= 50 ? point.y - 12 : point.y + 12
}
const indoorConnectionActive = (connection) => (
  indoorRouteConnectionKeys.value.has(indoorRouteKey(connection.fromNodeId, connection.toNodeId))
)
const indoorPointActive = (point) => indoorRouteNodeIds.value.has(point.id)
const indoorRouteFloorSet = computed(() => new Set(
  (indoorResult.value?.steps || []).flatMap((step) => [step.fromFloor, step.toFloor])
))
const indoorOverviewFloors = computed(() => (
  [...(selectedIndoorBuilding.value?.floors || [])].sort((a, b) => b - a)
))
const activeIndoorFloor = computed(() => (
  selectedIndoorFloor.value
    || indoorResult.value?.floorSegments?.[0]?.floor
    || selectedIndoorBuilding.value?.floors?.[0]
    || null
))
const activeIndoorFloorPoints = computed(() => indoorFloorPoints(activeIndoorFloor.value))
const activeIndoorFloorConnections = computed(() => indoorFloorConnections(activeIndoorFloor.value))
const activeIndoorFloorNormalConnections = computed(() => (
  activeIndoorFloorConnections.value.filter((connection) => !indoorConnectionActive(connection))
))
const activeIndoorFloorRouteConnections = computed(() => (
  activeIndoorFloorConnections.value.filter((connection) => indoorConnectionActive(connection))
))
const activeIndoorFloorSegment = computed(() => (
  indoorResult.value?.floorSegments?.find((segment) => segment.floor === activeIndoorFloor.value) || null
))
const indoorFloorHasRoute = (floor) => indoorRouteFloorSet.value.has(floor)
const selectIndoorFloor = (floor) => {
  selectedIndoorFloor.value = floor
}

const routeStageStats = computed(() => {
  const segments = routeResult.value?.segments || []
  const stages = []
  let currentSpotStage = null

  const flushSpotStage = () => {
    if (currentSpotStage && (currentSpotStage.distance > 0 || currentSpotStage.time > 0)) {
      stages.push(currentSpotStage)
    }
    currentSpotStage = null
  }

  segments.forEach((segment) => {
    if (segment.type === 'city') {
      flushSpotStage()
      stages.push({
        title: `${segment.fromSpotName} → ${segment.toSpotName}`,
        modeLabel: segment.transitType || '城市交通',
        distance: segment.distance || 0,
        time: segment.time || 0,
      })
      return
    }

    const spotName = segment.fromSpotName || segment.toSpotName || '景区内'
    const modeLabel = segment.transportMode === 'bike' ? '电动车' : '步行'
    if (!currentSpotStage || currentSpotStage.spotName !== spotName || currentSpotStage.modeLabel !== modeLabel) {
      flushSpotStage()
      currentSpotStage = {
        spotName,
        title: `${spotName} 景区内游览`,
        modeLabel,
        distance: 0,
        time: 0,
      }
    }
    currentSpotStage.distance += segment.distance || 0
    currentSpotStage.time += segment.time || 0
  })

  flushSpotStage()

  return stages.map((stage, index) => ({
    ...stage,
    index: index + 1,
    distanceKm: ((stage.distance || 0) / 1000).toFixed(2),
    timeMin: ((stage.time || 0) / 60).toFixed(1),
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
    ].filter((place) => isPublicFacilityPlace(place.name, place.type))
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
    const { data } = await searchNavSpots({ keyword, limit: 5 })
    for (const spot of data || []) {
      if (spot?.name && !selected.some((item) => item.name === spot.name)) {
        selected.push(spot)
      }
      if (selected.length >= 3) return selected
    }
  }
  return selected
}

const chooseDemoNodeIds = (visit) => {
  const options = nodeOptions(visit)
  return options.slice(0, 3).map((item) => item.value).filter((value) => value != null).reverse()
}

const applyDemoScenario = async () => {
  demoLoading.value = true
  routeResult.value = null
  clearPolylines()
  clearMarkers()
  try {
    const spots = await findDemoSpots()
    if (spots.length < 3) {
      ElMessage.warning('没有找到 3 个可用于演示的景区数据，请手动搜索景区')
      return
    }
    visits.value = spots.slice(0, 3).map((spot, index) => ({
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
    ElMessage.success('演示路线已填充 3 个景点，并开启景区与地点顺序优化')
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
  startNodeId: visits.value.find((visit) => visit.nodeIds?.length)?.nodeIds?.[0] || null,
  returnToStart: returnToStart.value,
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

const onIndoorBuildingChange = (buildingId) => {
  const building = indoorBuildings.value.find((item) => item.id === buildingId)
  indoorResult.value = null
  indoorForm.value.fromNodeId = building?.defaultFromNodeId || ''
  indoorForm.value.toNodeId = building?.defaultToNodeId || ''
  selectedIndoorFloor.value = building?.floors?.[0] || null
}

const loadIndoorBuildings = async () => {
  try {
    const { data } = await listIndoorBuildings()
    indoorBuildings.value = Array.isArray(data) ? data : []
    if (indoorBuildings.value.length && !indoorForm.value.buildingId) {
      indoorForm.value.buildingId = indoorBuildings.value[0].id
      onIndoorBuildingChange(indoorForm.value.buildingId)
    }
  } catch (error) {
    console.error(error)
    ElMessage.warning('室内导航演示数据加载失败')
  }
}

const applyIndoorDemo = async (building) => {
  if (!building) return
  indoorForm.value.buildingId = building.id
  indoorForm.value.fromNodeId = building.defaultFromNodeId
  indoorForm.value.toNodeId = building.defaultToNodeId
  await submitIndoor()
}

const submitIndoor = async () => {
  if (!indoorForm.value.buildingId || !indoorForm.value.fromNodeId || !indoorForm.value.toNodeId) {
    ElMessage.warning('请选择建筑、起点和终点')
    return
  }
  indoorLoading.value = true
  try {
    const { data } = await planIndoorRoute({ ...indoorForm.value })
    indoorResult.value = data
    selectedIndoorFloor.value = data?.floorSegments?.[0]?.floor || selectedIndoorFloor.value
    ElMessage.success('室内导航路线已生成')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '室内导航规划失败')
  } finally {
    indoorLoading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  await loadIndoorBuildings()
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
          active-text="优化景区与地点顺序"
          inactive-text="按选择顺序访问"
        />
        <div class="toolbar-actions">
          <el-switch
            v-model="returnToStart"
            active-text="返回起点"
            inactive-text="不返回"
          />
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

      <section class="indoor-demo-panel">
        <div class="indoor-demo-head">
          <div>
            <p class="eyebrow">Indoor navigation demo</p>
            <h3>室内导航策略演示</h3>
            <p>模拟教学楼、景区博物馆和游客中心内部结构，覆盖大门到电梯、楼层间电梯、楼层内到房间。</p>
          </div>
          <div class="indoor-demo-buttons">
            <el-button class="indoor-toggle-button" @click="indoorExpanded = !indoorExpanded">
              {{ indoorExpanded ? '收起演示' : '展开演示' }}
            </el-button>
            <el-button
              v-for="building in indoorBuildings"
              :key="building.id"
              :loading="indoorLoading && indoorForm.buildingId === building.id"
              @click="applyIndoorDemo(building)"
            >
              {{ building.type }}
            </el-button>
          </div>
        </div>

        <div v-show="indoorExpanded" class="indoor-form-grid">
          <label class="field-label indoor-field">
            演示建筑
            <el-select v-model="indoorForm.buildingId" class="full-width" @change="onIndoorBuildingChange">
              <el-option
                v-for="building in indoorBuildings"
                :key="building.id"
                :label="building.name"
                :value="building.id"
              />
            </el-select>
          </label>
          <label class="field-label indoor-field">
            起点
            <el-select v-model="indoorForm.fromNodeId" class="full-width" filterable>
              <el-option
                v-for="point in indoorPointOptions"
                :key="`from-${point.value}`"
                :label="point.label"
                :value="point.value"
              />
            </el-select>
          </label>
          <label class="field-label indoor-field">
            终点
            <el-select v-model="indoorForm.toNodeId" class="full-width" filterable>
              <el-option
                v-for="point in indoorPointOptions"
                :key="`to-${point.value}`"
                :label="point.label"
                :value="point.value"
              />
            </el-select>
          </label>
          <el-button type="primary" :loading="indoorLoading" @click="submitIndoor">
            <MapRoad theme="outline" size="16" fill="currentColor" />
            生成室内路线
          </el-button>
        </div>

        <section v-if="indoorExpanded && selectedIndoorBuilding" class="indoor-map-board">
          <div class="indoor-map-title">
            <strong>立体楼层导航图</strong>
            <span>左侧点击楼层切换视角；右侧放大显示该层地点、通道和 Dijkstra 最短路径。</span>
          </div>
          <div class="indoor-map-layout">
            <div class="indoor-stack" aria-label="立体楼层总览">
              <button
                v-for="(floor, stackIndex) in indoorOverviewFloors"
                :key="`stack-floor-${floor}`"
                type="button"
                class="indoor-stack-floor"
                :class="{ active: activeIndoorFloor === floor, route: indoorFloorHasRoute(floor) }"
                :style="{ '--stack-index': stackIndex }"
                @click="selectIndoorFloor(floor)"
              >
                <span class="floor-badge">{{ floor }}F</span>
                <span class="floor-name">{{ indoorFloorHasRoute(floor) ? '路线经过' : '可选楼层' }}</span>
                <span class="floor-count">{{ indoorFloorPoints(floor).length }} 个地点</span>
              </button>
            </div>

            <article class="indoor-detail-map">
              <div class="indoor-detail-head">
                <strong>{{ activeIndoorFloor }}F 放大路线图</strong>
                <span v-if="activeIndoorFloorSegment">{{ Math.round(activeIndoorFloorSegment.distance || 0) }} m</span>
                <span v-else>未经过当前路线</span>
              </div>
              <svg viewBox="0 0 100 100" role="img" :aria-label="`${activeIndoorFloor}F 室内节点图`">
                <line
                  v-for="connection in activeIndoorFloorNormalConnections"
                  :key="`normal-${connection.fromNodeId}-${connection.toNodeId}`"
                  :x1="indoorPointX(connection.fromNodeId)"
                  :y1="indoorPointY(connection.fromNodeId)"
                  :x2="indoorPointX(connection.toNodeId)"
                  :y2="indoorPointY(connection.toNodeId)"
                  class="indoor-map-link"
                />
                <line
                  v-for="connection in activeIndoorFloorRouteConnections"
                  :key="`route-${connection.fromNodeId}-${connection.toNodeId}`"
                  :x1="indoorPointX(connection.fromNodeId)"
                  :y1="indoorPointY(connection.fromNodeId)"
                  :x2="indoorPointX(connection.toNodeId)"
                  :y2="indoorPointY(connection.toNodeId)"
                  class="indoor-map-link active"
                />
                <g
                  v-for="point in activeIndoorFloorPoints"
                  :key="point.id"
                  class="indoor-map-node"
                  :class="{ active: indoorPointActive(point), elevator: point.type === 'elevator' }"
                >
                  <line
                    class="indoor-map-label-leader"
                    :x1="point.x"
                    :y1="point.y"
                    :x2="indoorLabelX(point)"
                    :y2="indoorLabelY(point)"
                  />
                  <circle :cx="point.x" :cy="point.y" r="3.25" />
                  <rect
                    class="indoor-map-label-bg"
                    :x="indoorLabelX(point) - indoorLabelWidth(point.name) / 2"
                    :y="indoorLabelY(point) - 3.3"
                    :width="indoorLabelWidth(point.name)"
                    height="6.6"
                    rx="3.3"
                  />
                  <text
                    class="indoor-map-label"
                    :x="indoorLabelX(point)"
                    :y="indoorLabelY(point) + 0.95"
                    text-anchor="middle"
                  >
                    {{ point.name }}
                  </text>
                </g>
              </svg>
            </article>
          </div>
        </section>

        <div v-if="indoorExpanded && indoorResult" class="indoor-result">
          <div class="indoor-result-title">
            <strong>{{ indoorResult.buildingName }}</strong>
            <span>{{ indoorResult.fromName }} → {{ indoorResult.toName }}</span>
            <em>{{ indoorDistanceText }}</em>
          </div>
          <div class="indoor-floor-grid">
            <article v-for="floor in indoorResult.floorSegments" :key="floor.floor" class="indoor-floor">
              <strong>{{ floor.title }}</strong>
              <span>{{ floor.pointNames.join(' → ') }}</span>
            </article>
          </div>
          <ol class="indoor-steps">
            <li v-for="(step, index) in indoorResult.steps" :key="`${step.fromNodeId}-${step.toNodeId}-${index}`">
              <b>{{ index + 1 }}</b>
              <span>{{ step.instruction }}</span>
              <em>{{ Math.round(step.distance || 0) }} m</em>
            </li>
          </ol>
        </div>
      </section>

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
                  placeholder="选择地点、POI 或路网节点"
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
          <span><Ranking theme="outline" size="15" fill="currentColor" /> 行程段数</span>
          <strong>{{ routeStageStats.length }}</strong>
        </article>
      </section>

      <div v-if="routeResult" class="segment-list">
        <div v-for="stage in routeStageStats" :key="stage.index" class="segment-item">
          <strong>{{ stage.index }}. {{ stage.title }}</strong>
          <span>{{ stage.modeLabel }}</span>
          <span>{{ stage.distanceKm }} km</span>
          <span>{{ stage.timeMin }} 分钟</span>
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
  align-items: center;
  gap: 10px;
}

.indoor-demo-panel {
  display: grid;
  gap: 16px;
  margin: 0 0 18px;
  padding: 18px;
  border-radius: 22px;
  color: #e5e7eb;
  background:
    linear-gradient(135deg, rgba(17, 24, 39, 0.96), rgba(36, 38, 45, 0.92)),
    #111827;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}

.indoor-demo-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.indoor-demo-head h3 {
  margin: 0;
  color: #ffffff;
  font-size: 22px;
}

.indoor-demo-head p:not(.eyebrow) {
  margin: 8px 0 0;
  color: #a7b0bf;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
}

.indoor-demo-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.indoor-toggle-button {
  min-width: 92px;
}

.indoor-form-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) minmax(160px, 1fr) minmax(160px, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.indoor-field {
  margin: 0;
  color: #cbd5e1;
}

.indoor-demo-panel :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.16) inset;
}

.indoor-demo-panel :deep(.el-input__inner) {
  color: #f8fafc;
  font-weight: 700;
}

.indoor-map-board {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.indoor-map-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.indoor-map-title strong {
  color: #ffffff;
  font-size: 16px;
}

.indoor-map-title span {
  color: #cbd5e1;
  font-size: 12px;
  font-weight: 800;
}

.indoor-map-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.85fr) minmax(360px, 1.15fr);
  gap: 18px;
  align-items: stretch;
}

.indoor-stack {
  position: relative;
  min-height: 330px;
  display: grid;
  place-items: center;
  perspective: 900px;
  border-radius: 18px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.88), rgba(15, 118, 110, 0.18)),
    #111827;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.indoor-stack::before {
  content: '';
  position: absolute;
  inset: 20px;
  border-radius: 18px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 22px 22px;
}

.indoor-stack-floor {
  position: absolute;
  width: min(76%, 360px);
  height: 96px;
  left: 10%;
  bottom: calc(36px + var(--stack-index) * 64px);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  color: #e5e7eb;
  background: linear-gradient(135deg, rgba(226, 232, 240, 0.18), rgba(148, 163, 184, 0.06));
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.22);
  transform: rotateX(58deg) rotateZ(-12deg) translateX(calc(var(--stack-index) * 16px));
  transform-origin: center;
  cursor: pointer;
}

.indoor-stack-floor.route {
  border-color: rgba(255, 56, 92, 0.75);
  background: linear-gradient(135deg, rgba(255, 56, 92, 0.36), rgba(255, 255, 255, 0.08));
}

.indoor-stack-floor.active {
  color: #ffffff;
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(255, 56, 92, 0.45), 0 26px 48px rgba(255, 56, 92, 0.18);
}

.floor-badge {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 14px;
  color: #111827;
  background: #ffffff;
  font-weight: 950;
}

.floor-name,
.floor-count {
  font-weight: 900;
}

.floor-count {
  color: #cbd5e1;
  font-size: 12px;
}

.indoor-detail-map {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(15, 23, 42, 0.98), rgba(25, 44, 72, 0.94)),
    #111827;
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.indoor-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.indoor-detail-head strong {
  color: #f8fafc;
  font-size: 16px;
}

.indoor-detail-head span {
  padding: 5px 10px;
  border-radius: 999px;
  color: #ffffff;
  background: rgba(255, 56, 92, 0.9);
  font-size: 12px;
  font-weight: 900;
}

.indoor-detail-map svg {
  width: 100%;
  min-height: 310px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 20% 18%, rgba(56, 189, 248, 0.2), transparent 28%),
    radial-gradient(circle at 78% 72%, rgba(255, 56, 92, 0.16), transparent 30%),
    linear-gradient(145deg, #17243a, #0b1322 72%);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
  overflow: visible;
}

.indoor-map-link {
  stroke: #6f8fb5;
  stroke-width: 2;
  stroke-linecap: round;
  opacity: 0.78;
}

.indoor-map-link.active {
  stroke: #ff385c;
  stroke-width: 4.2;
  opacity: 1;
  filter: drop-shadow(0 0 3px rgba(255, 56, 92, 0.7));
}

.indoor-map-label-leader {
  stroke: rgba(203, 213, 225, 0.42);
  stroke-width: 0.55;
  stroke-dasharray: 1.2 1.2;
}

.indoor-map-node circle {
  fill: #dbeafe;
  stroke: #0f172a;
  stroke-width: 1.6;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.35));
}

.indoor-map-node.elevator circle {
  fill: #14b8a6;
}

.indoor-map-node.active circle {
  fill: #ff385c;
  stroke: #ffffff;
}

.indoor-map-label-bg {
  fill: rgba(15, 23, 42, 0.78);
  stroke: rgba(148, 163, 184, 0.42);
  stroke-width: 0.45;
}

.indoor-map-node.active .indoor-map-label-bg {
  fill: rgba(255, 56, 92, 0.9);
  stroke: rgba(255, 255, 255, 0.45);
}

.indoor-map-label {
  fill: #e2e8f0;
  font-size: 2.35px;
  font-weight: 850;
  letter-spacing: 0;
  pointer-events: none;
}

.indoor-map-node.active .indoor-map-label {
  fill: #ffffff;
}

.indoor-result {
  display: grid;
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.indoor-result-title {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.indoor-result-title strong {
  color: #ffffff;
  font-size: 18px;
}

.indoor-result-title span {
  color: #cbd5e1;
  font-weight: 800;
}

.indoor-result-title em {
  margin-left: auto;
  padding: 6px 10px;
  border-radius: 999px;
  color: #111827;
  background: #ffffff;
  font-style: normal;
  font-weight: 900;
}

.indoor-floor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.indoor-floor {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 16px;
  color: #111827;
  background: #f8fafc;
}

.indoor-floor strong {
  color: #ff385c;
}

.indoor-floor span {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.5;
}

.indoor-steps {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.indoor-steps li {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
}

.indoor-steps b {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 10px;
  color: #ffffff;
  background: #ff385c;
}

.indoor-steps span {
  color: #f8fafc;
  font-weight: 800;
}

.indoor-steps em {
  color: #a7b0bf;
  font-style: normal;
  font-size: 12px;
  font-weight: 800;
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
  .indoor-demo-head,
  .indoor-result-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .indoor-demo-buttons {
    justify-content: flex-start;
  }

  .indoor-form-grid,
  .indoor-floor-grid,
  .indoor-map-layout,
  .indoor-steps li {
    grid-template-columns: 1fr;
  }

  .indoor-stack {
    min-height: 280px;
  }

  .indoor-stack-floor {
    width: 74%;
    height: 82px;
    bottom: calc(28px + var(--stack-index) * 54px);
  }

  .indoor-detail-map svg {
    min-height: 240px;
  }

  .indoor-result-title em {
    margin-left: 0;
  }

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
