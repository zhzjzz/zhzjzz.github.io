<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getNavNodes, listNavBuildingsBySpot, listNavPoisBySpot, planCrossSpotRoute, searchNavSpots } from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'

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

const mapInstance = ref(null)
const mapApi = ref(null)
const drivingInstance = ref(null)
const polylineLayers = ref([])
const markerOverlays = ref([])
const loading = ref(false)
const nodeLoading = ref(false)
const routeResult = ref(null)

const startInput = ref('')
const startSelection = ref(null)
const startNodes = ref([])
const startPlaces = ref([])
const startNodeId = ref(null)

const destInput = ref('')
const destSelection = ref(null)
const destNodes = ref([])
const destPlaces = ref([])
const destNodeId = ref(null)

const formatNode = (node) => {
  const floor = node.floor == null ? '' : ` F${node.floor}`
  return `${node.osmid}${floor}`
}

const nodeOptions = (places, nodes) => {
  const placeOptions = places
    .filter((place) => place.nearestNodeId != null)
    .map((place) => ({
      label: `${place.name || place.nearestNodeId} · ${place.type || '地点'}`,
      value: place.nearestNodeId,
      raw: place,
    }))
  const seen = new Set(placeOptions.map((item) => item.value))
  const rawNodeOptions = nodes
    .filter((node) => node.osmid != null && !seen.has(node.osmid))
    .slice(0, 200)
    .map((node) => ({
      label: `路网节点 ${formatNode(node)}`,
      value: node.osmid,
      raw: node,
    }))
  return [...placeOptions, ...rawNodeOptions]
}

const startNodeOptions = computed(() => nodeOptions(startPlaces.value, startNodes.value))
const destNodeOptions = computed(() => nodeOptions(destPlaces.value, destNodes.value))

const distanceKm = computed(() => ((routeResult.value?.totalDistance || 0) / 1000).toFixed(2))
const durationMin = computed(() => ((routeResult.value?.totalTime || 0) / 60).toFixed(1))

const clearPolylines = () => {
  if (polylineLayers.value.length && mapInstance.value) {
    mapInstance.value.remove(polylineLayers.value)
  }
  polylineLayers.value = []
  if (drivingInstance.value) {
    drivingInstance.value.clear()
    drivingInstance.value = null
  }
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
      background: '#222222',
      color: '#ffffff',
      border: 'none',
      borderRadius: '999px',
      padding: '4px 10px',
      fontSize: '12px',
      fontWeight: 600,
      boxShadow: '0 6px 18px rgba(0,0,0,0.2)',
    },
    zIndex: 130,
  })
  return [marker, text]
}

const drawMicroPath = (coords, color) => {
  if (!mapInstance.value || !mapApi.value || !coords?.length) return null
  const gcjPath = wgs84ToGcj02Batch(coords)
  const amapPath = gcjPath.map(([lat, lng]) => [lng, lat])
  const polyline = new mapApi.value.Polyline({
    path: amapPath,
    strokeColor: color,
    strokeWeight: 7,
    strokeOpacity: 0.94,
    isOutline: true,
    outlineColor: '#ffffff',
    lineJoin: 'round',
    showDir: true,
  })
  mapInstance.value.add(polyline)
  return polyline
}

const drawCityRoute = (startCoord, endCoord) => {
  if (!mapInstance.value || !mapApi.value || !startCoord || !endCoord) return
  const [startLat, startLng] = startCoord
  const [endLat, endLng] = endCoord
  const [gcjStartLng, gcjStartLat] = wgs84ToGcj02(startLng, startLat)
  const [gcjEndLng, gcjEndLat] = wgs84ToGcj02(endLng, endLat)

  drivingInstance.value?.clear()
  drivingInstance.value = new mapApi.value.Driving({
    map: mapInstance.value,
    hideMarkers: true,
  })
  drivingInstance.value.search(
    new mapApi.value.LngLat(gcjStartLng, gcjStartLat),
    new mapApi.value.LngLat(gcjEndLng, gcjEndLat)
  )
}

const selectedStartNode = computed(() =>
  startNodes.value.find((node) => node.osmid === startNodeId.value)
)
const selectedDestNode = computed(() =>
  destNodes.value.find((node) => node.osmid === destNodeId.value)
)

const drawMarkers = () => {
  if (!mapInstance.value || !mapApi.value) return
  clearMarkers()

  if (selectedStartNode.value) {
    const [gcjLng, gcjLat] = wgs84ToGcj02(selectedStartNode.value.x, selectedStartNode.value.y)
    markerOverlays.value.push(
      ...createMarker({ lng: gcjLng, lat: gcjLat, color: '#ff385c', title: `起点 ${startSelection.value?.name || ''}` })
    )
  }
  if (selectedDestNode.value) {
    const [gcjLng, gcjLat] = wgs84ToGcj02(selectedDestNode.value.x, selectedDestNode.value.y)
    markerOverlays.value.push(
      ...createMarker({ lng: gcjLng, lat: gcjLat, color: '#222222', title: `终点 ${destSelection.value?.name || ''}` })
    )
  }
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

const loadNodesForSpot = async (spotName, targetNodes, targetPlaces, targetNodeId) => {
  nodeLoading.value = true
  try {
    const [nodesRes, buildingsRes, poisRes] = await Promise.all([
      getNavNodes(spotName),
      listNavBuildingsBySpot(spotName),
      listNavPoisBySpot(spotName),
    ])
    targetNodes.value = Array.isArray(nodesRes.data) ? nodesRes.data : []
    targetPlaces.value = [
      ...(Array.isArray(buildingsRes.data) ? buildingsRes.data : []),
      ...(Array.isArray(poisRes.data) ? poisRes.data : []),
    ]
    targetNodeId.value = targetPlaces.value.find((item) => item.nearestNodeId != null)?.nearestNodeId
      || targetNodes.value[0]?.osmid
      || null
  } finally {
    nodeLoading.value = false
  }
}

const onStartSelect = async (item) => {
  startSelection.value = item.raw
  startInput.value = item.value
  startNodes.value = []
  startPlaces.value = []
  startNodeId.value = null
  await loadNodesForSpot(item.raw.name, startNodes, startPlaces, startNodeId)
  drawMarkers()
  refreshMapView()
}

const onStartInput = (value) => {
  if (startSelection.value && value !== startSelection.value.name) {
    startSelection.value = null
    startNodes.value = []
    startPlaces.value = []
    startNodeId.value = null
    drawMarkers()
  }
}

const onDestSelect = async (item) => {
  destSelection.value = item.raw
  destInput.value = item.value
  destNodes.value = []
  destPlaces.value = []
  destNodeId.value = null
  await loadNodesForSpot(item.raw.name, destNodes, destPlaces, destNodeId)
  drawMarkers()
  refreshMapView()
}

const onDestInput = (value) => {
  if (destSelection.value && value !== destSelection.value.name) {
    destSelection.value = null
    destNodes.value = []
    destPlaces.value = []
    destNodeId.value = null
    drawMarkers()
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
  } catch (error) {
    callback([])
  }
}

const submit = async () => {
  if (!startSelection.value) {
    ElMessage.warning('请先选择起点景区')
    return
  }
  if (!destSelection.value) {
    ElMessage.warning('请先选择终点景区')
    return
  }

  loading.value = true
  routeResult.value = null
  clearPolylines()

  try {
    const { data } = await planCrossSpotRoute({
      fromSpotName: startSelection.value.name,
      fromNodeId: startNodeId.value,
      toSpotName: destSelection.value.name,
      toNodeId: destNodeId.value,
      strategy: 'SHORTEST_DISTANCE',
      transportMode: 'walk',
    })
    routeResult.value = data

    if (data.microPathStart?.length) {
      const p1 = drawMicroPath(data.microPathStart, '#ff385c')
      if (p1) polylineLayers.value.push(p1)
    }
    if (data.cityTransitStart && data.cityTransitEnd) {
      drawCityRoute(data.cityTransitStart, data.cityTransitEnd)
    }
    if (data.microPathEnd?.length) {
      const p3 = drawMicroPath(data.microPathEnd, '#222222')
      if (p3) polylineLayers.value.push(p3)
    }

    drawMarkers()
    refreshMapView()
    ElMessage.success('跨景区路线规划完成')
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
      <div class="module-header">
        <div>
          <h2>三段式跨景区导航</h2>
          <p class="module-subtitle">景区内路径 + 城市交通 + 目标景区内路径</p>
        </div>
      </div>

      <el-form label-width="86px" class="route-form">
        <el-row :gutter="12">
          <el-col :md="8" :xs="24">
            <el-form-item label="起点景区">
              <el-autocomplete
                v-model="startInput"
                class="full-width"
                clearable
                placeholder="搜索景区/高校"
                :fetch-suggestions="fetchSpotSuggestions"
                @select="onStartSelect"
                @input="onStartInput"
              />
            </el-form-item>
          </el-col>
          <el-col :md="6" :xs="24">
            <el-form-item label="起点节点">
              <el-select
                v-model="startNodeId"
                class="full-width"
                clearable
                filterable
                :loading="nodeLoading"
                placeholder="默认入口节点"
                @change="drawMarkers"
              >
                <el-option v-for="item in startNodeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="终点景区">
              <el-autocomplete
                v-model="destInput"
                class="full-width"
                clearable
                placeholder="搜索景区/高校"
                :fetch-suggestions="fetchSpotSuggestions"
                @select="onDestSelect"
                @input="onDestInput"
              />
            </el-form-item>
          </el-col>
          <el-col :md="6" :xs="24">
            <el-form-item label="终点节点">
              <el-select
                v-model="destNodeId"
                class="full-width"
                clearable
                filterable
                :loading="nodeLoading"
                placeholder="默认入口节点"
                @change="drawMarkers"
              >
                <el-option v-for="item in destNodeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="4" :xs="24">
            <el-button type="primary" :loading="loading" class="plan-btn" @click="submit">规划路线</el-button>
          </el-col>
        </el-row>
      </el-form>

      <el-divider />
      <div class="map-title">
        <span>高德地图 · 三段式导航</span>
      </div>
      <div id="route-map" class="route-map" />

      <el-divider />
      <el-descriptions v-if="routeResult" :column="3" border>
        <el-descriptions-item label="总距离">{{ distanceKm }} km</el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ durationMin }} 分钟</el-descriptions-item>
        <el-descriptions-item label="城市交通">{{ routeResult.transitType || '无，同景区内直达' }}</el-descriptions-item>
        <el-descriptions-item label="第一段距离">{{ ((routeResult.segment1Distance || 0) / 1000).toFixed(2) }} km</el-descriptions-item>
        <el-descriptions-item label="第二段距离">{{ ((routeResult.segment2Distance || 0) / 1000).toFixed(2) }} km</el-descriptions-item>
        <el-descriptions-item label="第三段距离">{{ ((routeResult.segment3Distance || 0) / 1000).toFixed(2) }} km</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="选择起点和终点后规划路线" />
    </el-card>
  </section>
</template>

<style scoped>
.full-width {
  width: 100%;
}

.route-form {
  max-width: 1180px;
}

.plan-btn {
  width: 100%;
}

.route-map {
  width: 100%;
  height: 520px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 56, 92, 0.18);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #fff8f8 0%, #f7f7f7 100%);
}

.map-title {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff1f4;
  color: #ff385c;
  font-size: 13px;
  font-weight: 600;
}
</style>
