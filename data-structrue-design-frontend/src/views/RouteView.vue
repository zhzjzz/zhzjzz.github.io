<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getOsmRoute, searchRouteDestinations } from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'

const CHINA_CENTER = [104.1954, 35.8617]
const DEFAULT_ZOOM = 4
const AMAP_VERSION = '2.0'
const AMAP_PLUGINS = ['AMap.Scale', 'AMap.ToolBar']

const mapInstance = ref(null)
const mapApi = ref(null)
const routeLayer = ref(null)
const markerLayers = ref([])
const loading = ref(false)
const routeResult = ref(null)
const form = ref({
  mode: 'car',
})
const startInput = ref('')
const startSelection = ref(null)
const destinationStops = ref([
  {
    input: '',
    selection: null,
  },
])
const transportModes = [
  { value: 'car', label: '汽车' },
  { value: 'bike', label: '自行车' },
  { value: 'walk', label: '徒步' },
]

const selectedStops = computed(() =>
  destinationStops.value.filter((stop) => stop.selection).map((stop) => stop.selection)
)

const distanceKm = computed(() => ((routeResult.value?.distance || 0) / 1000).toFixed(2))
const durationHours = computed(() => ((routeResult.value?.time || 0) / 1000 / 3600).toFixed(2))

const clearRouteLayer = () => {
  if (routeLayer.value && mapInstance.value) {
    mapInstance.value.remove(routeLayer.value)
  }
  routeLayer.value = null
}

const clearMarkerLayers = () => {
  if (markerLayers.value.length && mapInstance.value) {
    mapInstance.value.remove(markerLayers.value)
  }
  markerLayers.value = []
}

const initMap = async () => {
  const amapKey = (import.meta.env.VITE_AMAP_KEY || '').trim()
  if (!amapKey) {
    ElMessage.warning('未配置 VITE_AMAP_KEY，无法加载高德地图')
    return
  }

  try {
    const AMap = await AMapLoader.load({
      key: amapKey,
      version: AMAP_VERSION,
      plugins: AMAP_PLUGINS,
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
    ElMessage.error('高德地图加载失败，请检查 Key 或网络')
  }
}

const drawRoute = (path) => {
  if (!mapInstance.value || !mapApi.value) return
  clearRouteLayer()
  if (!path?.length) return

  const gcjPath = wgs84ToGcj02Batch(path)
  const amapPath = gcjPath.map(([lat, lng]) => [lng, lat])
  routeLayer.value = new mapApi.value.Polyline({
    path: amapPath,
    strokeColor: '#1677ff',
    strokeWeight: 7,
    strokeOpacity: 0.94,
    isOutline: true,
    outlineColor: '#ffffff',
    lineJoin: 'round',
    showDir: true,
  })
  mapInstance.value.add(routeLayer.value)
  mapInstance.value.setFitView([routeLayer.value], false, [70, 70, 70, 70], 17)
}

const createPointOverlay = ({ lng, lat, color, title }) => {
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
      color: '#f9fafb',
      border: 'none',
      borderRadius: '999px',
      padding: '4px 10px',
      fontSize: '12px',
      fontWeight: 600,
      boxShadow: '0 6px 18px rgba(17,24,39,0.25)',
    },
    zIndex: 130,
  })
  return [marker, text]
}

const drawMarkers = () => {
  if (!mapInstance.value || !mapApi.value) return
  clearMarkerLayers()

  if (startSelection.value) {
    const [gcjLng, gcjLat] = wgs84ToGcj02(startSelection.value.longitude, startSelection.value.latitude)
    markerLayers.value.push(
      ...createPointOverlay({
        lng: gcjLng,
        lat: gcjLat,
        color: '#1677ff',
        title: `起点：${startSelection.value.name}`,
      })
    )
  }

  selectedStops.value.forEach((destination, index) => {
    const isLast = index === selectedStops.value.length - 1
    const [gcjLng, gcjLat] = wgs84ToGcj02(destination.longitude, destination.latitude)
    markerLayers.value.push(
      ...createPointOverlay({
        lng: gcjLng,
        lat: gcjLat,
        color: isLast ? '#22c55e' : '#f59e0b',
        title: `${isLast ? '终点' : `途经点${index + 1}`}：${destination.name}`,
      })
    )
  })

  if (markerLayers.value.length) {
    mapInstance.value.add(markerLayers.value)
  }
}

const refreshMapView = () => {
  if (!mapInstance.value) return
  const overlays = [routeLayer.value, ...markerLayers.value].filter(Boolean)
  if (overlays.length) {
    mapInstance.value.setFitView(overlays, false, [70, 70, 70, 70], 17)
  } else {
    mapInstance.value.setZoomAndCenter(DEFAULT_ZOOM, CHINA_CENTER)
  }
}

const onStartSelect = (item) => {
  startSelection.value = item.raw
  startInput.value = item.value
  drawMarkers()
  refreshMapView()
}

const onStartInput = (value) => {
  if (startSelection.value && value !== startSelection.value.name) {
    startSelection.value = null
    drawMarkers()
    refreshMapView()
  }
}

const onDestinationSelect = (index, item) => {
  destinationStops.value[index].selection = item.raw
  destinationStops.value[index].input = item.value
  drawMarkers()
  refreshMapView()
}

const onDestinationInput = (index, value) => {
  const current = destinationStops.value[index]
  if (current.selection && value !== current.selection.name) {
    current.selection = null
    drawMarkers()
    refreshMapView()
  }
}

const fetchDestinationSuggestions = async (queryString, callback) => {
  const keyword = (queryString || '').trim()
  if (!keyword) {
    callback([])
    return
  }
  try {
    const { data } = await searchRouteDestinations(keyword, 10)
    callback(
      (data || [])
        .filter((item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude))
        .map((item) => ({
          value: item.name,
          raw: item,
        }))
    )
  } catch (error) {
    callback([])
  }
}

const addDestinationStop = () => {
  destinationStops.value.push({
    input: '',
    selection: null,
  })
}

const removeDestinationStop = (index) => {
  if (destinationStops.value.length <= 1) return
  destinationStops.value.splice(index, 1)
  drawMarkers()
  refreshMapView()
}

const submit = async () => {
  if (!startSelection.value) {
    ElMessage.warning('请先搜索并选择起点')
    return
  }
  if (!destinationStops.value.length || destinationStops.value.some((stop) => !stop.selection)) {
    ElMessage.warning('请搜索并选择所有目的地')
    return
  }

  loading.value = true
  routeResult.value = null
  try {
    let totalDistance = 0
    let totalTime = 0
    const mergedPath = []
    let currentPoint = startSelection.value

    for (let index = 0; index < destinationStops.value.length; index += 1) {
      const stop = destinationStops.value[index]
      const destination = stop.selection
      try {
        const { data } = await getOsmRoute({
          startLat: currentPoint.latitude,
          startLon: currentPoint.longitude,
          endLat: destination.latitude,
          endLon: destination.longitude,
          mode: form.value.mode,
        })
        totalDistance += Number(data?.distance ?? 0)
        totalTime += Number(data?.time ?? 0)
        const segmentPath = Array.isArray(data?.path) ? data.path : []
        if (segmentPath.length) {
          if (mergedPath.length) {
            mergedPath.push(...segmentPath.slice(1))
          } else {
            mergedPath.push(...segmentPath)
          }
        }
        currentPoint = destination
      } catch (error) {
        const segmentMessage = error?.response?.data?.message || error?.message || 'OSM 路线规划失败'
        throw new Error(`第 ${index + 1} 段（${destination?.name || `目的地${index + 1}`})规划失败：${segmentMessage}`)
      }
    }

    routeResult.value = {
      distance: totalDistance,
      time: totalTime,
      path: mergedPath,
    }
    drawRoute(mergedPath)
    drawMarkers()
    refreshMapView()
    ElMessage.success('路线规划成功')
  } catch (error) {
    ElMessage.error(error?.message || error?.response?.data?.message || '路线规划失败，请检查后端 GraphHopper 数据文件配置')
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
  clearRouteLayer()
  clearMarkerLayers()
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
          <h2>OSM 路线规划</h2>
          <p class="module-subtitle">支持搜索起点与多个目的地，按顺序进行分段路线规划并汇总总距离和时间。</p>
        </div>
      </div>

      <el-form :model="form" label-width="120px" class="route-form">
        <el-row :gutter="12">
          <el-col :md="8" :xs="24">
            <el-form-item label="起点">
              <el-autocomplete
                v-model="startInput"
                class="full-width"
                clearable
                placeholder="输入关键词搜索起点"
                :fetch-suggestions="fetchDestinationSuggestions"
                @select="onStartSelect"
                @input="onStartInput"
              />
            </el-form-item>
          </el-col>
          <el-col :md="16" :xs="24">
            <el-form-item label="多个目的地">
              <div class="destination-list">
                <div v-for="(stop, index) in destinationStops" :key="index" class="destination-item">
                  <el-autocomplete
                    v-model="stop.input"
                    class="full-width"
                    clearable
                    :placeholder="`输入关键词搜索目的地 ${index + 1}`"
                    :fetch-suggestions="fetchDestinationSuggestions"
                    @select="(item) => onDestinationSelect(index, item)"
                    @input="(value) => onDestinationInput(index, value)"
                  />
                  <el-button type="danger" plain :disabled="destinationStops.length <= 1" @click="removeDestinationStop(index)">删除</el-button>
                  <el-button v-if="index === destinationStops.length - 1" type="primary" plain @click="addDestinationStop">添加</el-button>
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="出行方式">
              <el-select v-model="form.mode" class="full-width" placeholder="请选择出行方式">
                <el-option
                  v-for="item in transportModes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" :loading="loading" @click="submit">规划路线</el-button>
      </el-form>

      <el-divider />

      <el-descriptions v-if="routeResult" :column="2" border>
        <el-descriptions-item label="总距离">{{ distanceKm }} km</el-descriptions-item>
        <el-descriptions-item label="预计耗时">{{ durationHours }} h</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="请搜索起点和多个目的地后点击规划路线" />

      <el-divider />
      <div class="map-title">
        <span>高德官方 JS API 地图</span>
      </div>
      <div id="route-map" class="route-map" />
    </el-card>
  </section>
</template>

<style scoped>
.full-width {
  width: 100%;
}

.route-form {
  max-width: 900px;
}

.destination-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.destination-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}

.route-map {
  width: 100%;
  height: 520px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.22);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
  background: linear-gradient(135deg, #e6f0ff 0%, #f8fbff 100%);
}

.map-title {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
}
</style>
