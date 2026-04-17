<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { getOsmRoute, listDestinations } from '../api/travel'

const CHINA_CENTER = [35.8617, 104.1954]
const DEFAULT_ZOOM = 4

const mapInstance = ref(null)
const routeLayer = ref(null)
const markerLayer = ref(null)
const loading = ref(false)
const destinations = ref([])
const routeResult = ref(null)
const form = ref({
  startId: null,
  endId: null,
})

const selectableEndDestinations = computed(() =>
  destinations.value.filter((item) => item.id !== form.value.startId)
)

const selectedStart = computed(() =>
  destinations.value.find((item) => item.id === form.value.startId) || null
)

const selectedEnd = computed(() =>
  destinations.value.find((item) => item.id === form.value.endId) || null
)

const distanceKm = computed(() => ((routeResult.value?.distance || 0) / 1000).toFixed(2))
const durationHours = computed(() => ((routeResult.value?.time || 0) / 1000 / 3600).toFixed(2))

const initMap = () => {
  mapInstance.value = L.map('route-map').setView(CHINA_CENTER, DEFAULT_ZOOM)
  L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 20,
    maxNativeZoom: 19,
    detectRetina: true,
    updateWhenZooming: false,
    keepBuffer: 8,
  }).addTo(mapInstance.value)
  markerLayer.value = L.layerGroup().addTo(mapInstance.value)
}

const drawRoute = (path) => {
  if (!mapInstance.value) return
  if (routeLayer.value) {
    mapInstance.value.removeLayer(routeLayer.value)
    routeLayer.value = null
  }
  if (!path?.length) return
  routeLayer.value = L.polyline(path, { color: '#ff385c', weight: 5 }).addTo(mapInstance.value)
  mapInstance.value.fitBounds(routeLayer.value.getBounds(), { padding: [24, 24] })
}

const drawMarkers = () => {
  if (!mapInstance.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  if (selectedStart.value) {
    L.circleMarker([selectedStart.value.latitude, selectedStart.value.longitude], {
      radius: 7,
      color: '#1677ff',
      fillColor: '#1677ff',
      fillOpacity: 1,
    })
      .bindPopup(`起点：${selectedStart.value.name}`)
      .addTo(markerLayer.value)
  }
  if (selectedEnd.value) {
    L.circleMarker([selectedEnd.value.latitude, selectedEnd.value.longitude], {
      radius: 7,
      color: '#22c55e',
      fillColor: '#22c55e',
      fillOpacity: 1,
    })
      .bindPopup(`终点：${selectedEnd.value.name}`)
      .addTo(markerLayer.value)
  }
}

const loadDestinations = async () => {
  const { data } = await listDestinations()
  destinations.value = (data || []).filter(
    (item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude)
  )
  if (destinations.value.length >= 2) {
    form.value.startId = destinations.value[0].id
    form.value.endId = destinations.value[1].id
  }
  drawMarkers()
}

const onStartChange = () => {
  if (form.value.startId === form.value.endId) {
    const nextEndId = selectableEndDestinations.value[0]?.id || null
    if (nextEndId !== form.value.endId) {
      form.value.endId = nextEndId
    }
  }
  drawMarkers()
}

const onEndChange = () => {
  drawMarkers()
}

const submit = async () => {
  if (!selectedStart.value || !selectedEnd.value) {
    ElMessage.warning('请选择起点和终点')
    return
  }
  loading.value = true
  routeResult.value = null
  try {
    const { data } = await getOsmRoute({
      startLat: selectedStart.value.latitude,
      startLon: selectedStart.value.longitude,
      endLat: selectedEnd.value.latitude,
      endLon: selectedEnd.value.longitude,
    })
    routeResult.value = data
    drawRoute(data.path)
    drawMarkers()
    ElMessage.success('路线规划成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '路线规划失败，请检查后端 GraphHopper 数据文件配置')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await nextTick()
  initMap()
  try {
    await loadDestinations()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载目的地失败')
  }
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
          <p class="module-subtitle">基于 GraphHopper + OpenStreetMap，按目的地经纬度计算真实路径。</p>
        </div>
      </div>

      <el-form :model="form" label-width="120px" class="route-form">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="起点">
              <el-select
                v-model="form.startId"
                class="full-width"
                placeholder="请选择起点"
                @change="onStartChange"
              >
                <el-option
                  v-for="item in destinations"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="终点">
              <el-select
                v-model="form.endId"
                class="full-width"
                placeholder="请选择终点"
                @change="onEndChange"
              >
                <el-option
                  v-for="item in selectableEndDestinations"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
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
      <el-empty v-else description="请选择起终点并点击规划路线" />

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

.route-map {
  width: 100%;
  height: 520px;
  border-radius: 16px;
  overflow: hidden;
}
</style>
