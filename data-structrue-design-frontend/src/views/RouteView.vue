<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getOsmRoute, searchRouteDestinations } from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'

const CHINA_CENTER = [35.8617, 104.1954]
const DEFAULT_ZOOM = 4

const mapInstance = ref(null)
const routeLayer = ref(null)
const markerLayer = ref(null)
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

const initMap = () => {
  mapInstance.value = L.map('route-map').setView(CHINA_CENTER, DEFAULT_ZOOM)
  // webrd0{s} + subdomains 1~4 用于轮询瓦片子域名，减少单域并发压力。
  L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    {
      attribution: '&copy; 高德地图',
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 18,
      maxNativeZoom: 18,
      updateWhenZooming: false,
      keepBuffer: 8,
    }
  ).addTo(mapInstance.value)
  markerLayer.value = L.layerGroup().addTo(mapInstance.value)
}

const drawRoute = (path) => {
  if (!mapInstance.value) return
  if (routeLayer.value) {
    mapInstance.value.removeLayer(routeLayer.value)
    routeLayer.value = null
  }
  if (!path?.length) return
  // 高德底图使用 GCJ-02 坐标系，需将 GraphHopper 返回的 WGS-84 坐标转换后绘制
  const gcjPath = wgs84ToGcj02Batch(path)
  routeLayer.value = L.polyline(gcjPath, { color: '#ff385c', weight: 5 }).addTo(mapInstance.value)
  mapInstance.value.fitBounds(routeLayer.value.getBounds(), { padding: [24, 24] })
}

const drawMarkers = () => {
  if (!mapInstance.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  if (startSelection.value) {
    // WGS-84 → GCJ-02 转换，消除高德底图偏移
    const [gcjLng, gcjLat] = wgs84ToGcj02(startSelection.value.longitude, startSelection.value.latitude)
    L.circleMarker([gcjLat, gcjLng], {
      radius: 7,
      color: '#1677ff',
      fillColor: '#1677ff',
      fillOpacity: 1,
    })
      .bindPopup(`起点：${startSelection.value.name}`)
      .addTo(markerLayer.value)
  }
  selectedStops.value.forEach((destination, index) => {
    const isLast = index === selectedStops.value.length - 1
    const [gcjLng, gcjLat] = wgs84ToGcj02(destination.longitude, destination.latitude)
    L.circleMarker([gcjLat, gcjLng], {
      radius: 7,
      color: isLast ? '#22c55e' : '#f59e0b',
      fillColor: isLast ? '#22c55e' : '#f59e0b',
      fillOpacity: 1,
    })
      .bindPopup(`${isLast ? '终点' : `途经点${index + 1}`}：${destination.name}`)
      .addTo(markerLayer.value)
  })
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

const onStartSelect = (item) => {
  startSelection.value = item.raw
  startInput.value = item.value
  drawMarkers()
}

const onStartInput = (value) => {
  if (startSelection.value && value !== startSelection.value.name) {
    startSelection.value = null
    drawMarkers()
  }
}

const onDestinationSelect = (index, item) => {
  destinationStops.value[index].selection = item.raw
  destinationStops.value[index].input = item.value
  drawMarkers()
}

const onDestinationInput = (index, value) => {
  const current = destinationStops.value[index]
  if (current.selection && value !== current.selection.name) {
    current.selection = null
    drawMarkers()
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
    ElMessage.success('路线规划成功')
  } catch (error) {
    ElMessage.error(error?.message || error?.response?.data?.message || '路线规划失败，请检查后端 GraphHopper 数据文件配置')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  initMap()
  mapInstance.value?.invalidateSize()
})

onBeforeUnmount(() => {
  if (mapInstance.value) {
    mapInstance.value.remove()
    mapInstance.value = null
  }
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
}

.route-map :deep(.leaflet-tile) {
  filter: saturate(1.25) contrast(1.12) brightness(0.9);
}
</style>
