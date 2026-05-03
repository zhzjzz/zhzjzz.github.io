<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { createItinerary, getItinerary, getOsmRoute, listItineraries, searchRouteDestinations, updateItinerary } from '../api/travel'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../utils/coordTransform'

const amapSecret = (import.meta.env.VITE_AMAP_SECRET || '').trim()
if (amapSecret) {
  window._AMapSecurityConfig = { securityJsCode: amapSecret }
}

const CHINA_CENTER = [104.1954, 35.8617]
const DEFAULT_ZOOM = 4
const AMAP_VERSION = '2.0'
const AMAP_PLUGINS = ['AMap.Scale', 'AMap.ToolBar']
const MAP_FIT_PADDING = [70, 70, 70, 70]
const MAP_MAX_ZOOM = 17
const MARKER_TEXT_BG_COLOR = '#111827'
const MARKER_TEXT_COLOR = '#f9fafb'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const routeLoading = ref(false)
const sharedId = ref('')

const mapInstance = ref(null)
const mapApi = ref(null)
const routeLayer = ref(null)
const markerOverlays = ref([])
const routeResult = ref(null)

const startInput = ref('')
const startSelection = ref(null)
const waypoints = ref([
  { input: '', selection: null },
])
const transportMode = ref('walk')
const transportModes = [
  { value: 'car', label: '汽车' },
  { value: 'bike', label: '自行车' },
  { value: 'walk', label: '徒步' },
]

const createForm = ref({
  name: '',
  owner: '演示用户',
  collaborators: 'admin',
  strategy: 'time',
  transportMode: 'walk+shuttle',
  notes: '',
})

const editForm = ref(null)
const serverSnapshot = ref(null)

const distanceKm = computed(() => ((routeResult.value?.distance || 0) / 1000).toFixed(2))
const durationHours = computed(() => ((routeResult.value?.time || 0) / 1000 / 3600).toFixed(2))

const toEditPayload = (itinerary) => ({
  id: itinerary.id,
  name: itinerary.name || '',
  owner: itinerary.owner || '',
  collaborators: itinerary.collaborators || '',
  strategy: itinerary.strategy || 'time',
  transportMode: itinerary.transportMode || '',
  notes: itinerary.notes || '',
  updatedAt: itinerary.updatedAt || null,
})

const selectedId = computed(() => editForm.value?.id ?? null)

const isEditingDirty = computed(() => {
  if (!editForm.value || !serverSnapshot.value) return false
  return JSON.stringify({
    name: editForm.value.name,
    owner: editForm.value.owner,
    collaborators: editForm.value.collaborators,
    strategy: editForm.value.strategy,
    transportMode: editForm.value.transportMode,
    notes: editForm.value.notes,
  }) !== JSON.stringify({
    name: serverSnapshot.value.name,
    owner: serverSnapshot.value.owner,
    collaborators: serverSnapshot.value.collaborators,
    strategy: serverSnapshot.value.strategy,
    transportMode: serverSnapshot.value.transportMode,
    notes: serverSnapshot.value.notes,
  })
})

const applyServerItinerary = (itinerary) => {
  const payload = toEditPayload(itinerary)
  editForm.value = payload
  serverSnapshot.value = { ...payload }
  try {
    const parsed = JSON.parse(itinerary.notes || '{}')
    if (parsed._waypoints && Array.isArray(parsed._waypoints)) {
      waypoints.value = parsed._waypoints.map(w => ({
        input: w.name || '',
        selection: { id: w.id, name: w.name, latitude: w.latitude, longitude: w.longitude },
      }))
    }
    if (parsed._start && parsed._start.name) {
      startSelection.value = { id: parsed._start.id, name: parsed._start.name, latitude: parsed._start.latitude, longitude: parsed._start.longitude }
      startInput.value = parsed._start.name
    }
    if (parsed._transportMode) transportMode.value = parsed._transportMode
    if (parsed._route && parsed._route.length) {
      routeResult.value = { distance: parsed._routeDistance || 0, time: parsed._routeTime || 0, path: parsed._route }
      drawRoute(parsed._route)
    }
  } catch { /* notes not JSON */ }
}

const load = async () => {
  loading.value = true
  try {
    const { data } = await listItineraries()
    list.value = data

    if (!selectedId.value) return

    const latest = data.find((item) => item.id === selectedId.value)
    if (!latest) {
      editForm.value = null
      serverSnapshot.value = null
      return
    }

    if (serverSnapshot.value?.updatedAt !== latest.updatedAt) {
      if (isEditingDirty.value) {
        ElMessage.warning('检测到其他客户端已更新该行程，请先保存或点击"刷新为最新版本"')
      } else {
        applyServerItinerary(latest)
      }
    }
  } finally {
    loading.value = false
  }
}

const openById = async () => {
  const id = Number(sharedId.value)
  if (!Number.isInteger(id) || id <= 0) {
    ElMessage.warning('请输入正确的行程ID')
    return
  }
  const { data } = await getItinerary(id)
  applyServerItinerary(data)
  if (!list.value.some((item) => item.id === id)) {
    list.value = [data, ...list.value]
  }
  ElMessage.success('已打开共享行程')
}

const selectItinerary = (row) => {
  applyServerItinerary(row)
  sharedId.value = String(row.id)
}

const refreshCurrent = async () => {
  if (!selectedId.value) return
  const { data } = await getItinerary(selectedId.value)
  applyServerItinerary(data)
  await load()
  ElMessage.success('已刷新为最新版本')
}

const submitCreate = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入行程名称')
    return
  }
  const { data } = await createItinerary(createForm.value)
  createForm.value = {
    name: '',
    owner: '演示用户',
    collaborators: 'admin',
    strategy: 'time',
    transportMode: 'walk+shuttle',
    notes: '',
  }
  ElMessage.success(`行程创建成功（ID: ${data.id}）`)
  await load()
}

const submitUpdate = async () => {
  if (!editForm.value) {
    ElMessage.warning('请先选择或打开行程')
    return
  }
  if (!editForm.value.name.trim()) {
    ElMessage.warning('行程名称不能为空')
    return
  }

  saving.value = true
  try {
    const routeData = {}
    if (startSelection.value) {
      routeData._start = {
        id: startSelection.value.id,
        name: startSelection.value.name,
        latitude: startSelection.value.latitude,
        longitude: startSelection.value.longitude,
      }
    }
    routeData._waypoints = waypoints.value
      .filter(w => w.selection)
      .map(w => ({
        id: w.selection.id,
        name: w.selection.name,
        latitude: w.selection.latitude,
        longitude: w.selection.longitude,
      }))
    routeData._transportMode = transportMode.value
    if (routeResult.value?.path?.length) {
      routeData._route = routeResult.value.path
      routeData._routeDistance = routeResult.value.distance
      routeData._routeTime = routeResult.value.time
    }
    const mergedNotes = routeData._waypoints.length || routeData._start
      ? JSON.stringify(routeData)
      : editForm.value.notes

    const payload = {
      name: editForm.value.name,
      owner: editForm.value.owner,
      collaborators: editForm.value.collaborators,
      strategy: editForm.value.strategy,
      transportMode: editForm.value.transportMode,
      notes: mergedNotes,
      updatedAt: editForm.value.updatedAt,
    }
    const { data } = await updateItinerary(editForm.value.id, payload)
    applyServerItinerary(data)
    await load()
    ElMessage.success('行程已保存，其他客户端将自动同步')
  } catch (error) {
    if (error?.response?.status === 409) {
      ElMessage.error('保存失败：该行程已被其他客户端修改，正在刷新最新版本')
      await refreshCurrent()
      return
    }
    throw error
  } finally {
    saving.value = false
  }
}

const clearRouteLayer = () => {
  if (routeLayer.value && mapInstance.value) {
    mapInstance.value.remove(routeLayer.value)
  }
  routeLayer.value = null
}

const clearMarkerLayers = () => {
  if (markerOverlays.value.length && mapInstance.value) {
    mapInstance.value.remove(markerOverlays.value)
  }
  markerOverlays.value = []
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
    mapInstance.value = new AMap.Map('itinerary-map', {
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

const drawRoute = (path) => {
  if (!mapInstance.value || !mapApi.value) return
  clearRouteLayer()
  if (!path?.length) return

  const gcjPath = wgs84ToGcj02Batch(path)
  const amapLngLatPath = gcjPath.map(([lat, lng]) => [lng, lat])
  routeLayer.value = new mapApi.value.Polyline({
    path: amapLngLatPath,
    strokeColor: '#1677ff',
    strokeWeight: 7,
    strokeOpacity: 0.94,
    isOutline: true,
    outlineColor: '#ffffff',
    lineJoin: 'round',
    showDir: true,
  })
  mapInstance.value.add(routeLayer.value)
  mapInstance.value.setFitView([routeLayer.value], false, MAP_FIT_PADDING, MAP_MAX_ZOOM)
}

const createMarkerWithLabel = ({ lng, lat, color, title }) => {
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
      background: MARKER_TEXT_BG_COLOR,
      color: MARKER_TEXT_COLOR,
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
    markerOverlays.value.push(
      ...createMarkerWithLabel({ lng: gcjLng, lat: gcjLat, color: '#1677ff', title: `起点：${startSelection.value.name}` })
    )
  }

  waypoints.value.forEach((wp, index) => {
    if (!wp.selection) return
    const isLast = waypoints.value.filter(w => w.selection).length - 1 === index || waypoints.value.slice(index + 1).every(w => !w.selection)
    const [gcjLng, gcjLat] = wgs84ToGcj02(wp.selection.longitude, wp.selection.latitude)
    markerOverlays.value.push(
      ...createMarkerWithLabel({
        lng: gcjLng, lat: gcjLat,
        color: isLast ? '#22c55e' : '#f59e0b',
        title: `${isLast ? '终点' : `途经点${index + 1}`}：${wp.selection.name}`,
      })
    )
  })

  if (markerOverlays.value.length) {
    mapInstance.value.add(markerOverlays.value)
  }
}

const refreshMapView = () => {
  if (!mapInstance.value) return
  const overlays = [routeLayer.value, ...markerOverlays.value].filter(Boolean)
  if (overlays.length) {
    mapInstance.value.setFitView(overlays, false, MAP_FIT_PADDING, MAP_MAX_ZOOM)
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

const onWaypointSelect = (index, item) => {
  waypoints.value[index].selection = item.raw
  waypoints.value[index].input = item.value
  drawMarkers()
  refreshMapView()
}

const onWaypointInput = (index, value) => {
  const current = waypoints.value[index]
  if (current.selection && value !== current.selection.name) {
    current.selection = null
    drawMarkers()
    refreshMapView()
  }
}

const fetchSuggestions = async (queryString, callback) => {
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
        .map((item) => ({ value: item.name, raw: item }))
    )
  } catch {
    callback([])
  }
}

const addWaypoint = () => {
  waypoints.value.push({ input: '', selection: null })
}

const removeWaypoint = (index) => {
  if (waypoints.value.length <= 1) return
  waypoints.value.splice(index, 1)
  drawMarkers()
  refreshMapView()
}

const planRoute = async () => {
  if (!startSelection.value) {
    ElMessage.warning('请先搜索并选择起点')
    return
  }
  const activeWaypoints = waypoints.value.filter(w => w.selection)
  if (!activeWaypoints.length) {
    ElMessage.warning('请至少搜索并选择一个目的地')
    return
  }

  routeLoading.value = true
  routeResult.value = null
  try {
    let totalDistance = 0
    let totalTime = 0
    const mergedPath = []
    let currentPoint = startSelection.value

    for (let i = 0; i < activeWaypoints.length; i++) {
      const destination = activeWaypoints[i].selection
      try {
        const { data } = await getOsmRoute({
          startLat: currentPoint.latitude,
          startLon: currentPoint.longitude,
          endLat: destination.latitude,
          endLon: destination.longitude,
          mode: transportMode.value,
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
        const msg = error?.response?.data?.message || error?.message || 'OSM 路线规划失败'
        throw new Error(`第 ${i + 1} 段（${destination?.name || `目的地${i + 1}`})规划失败：${msg}`)
      }
    }

    routeResult.value = { distance: totalDistance, time: totalTime, path: mergedPath }
    drawRoute(mergedPath)
    drawMarkers()
    refreshMapView()
    ElMessage.success('路线规划成功')
  } catch (error) {
    ElMessage.error(error?.message || '路线规划失败，请检查后端 GraphHopper 数据文件配置')
  } finally {
    routeLoading.value = false
  }
}

onMounted(async () => {
  await load()
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
  <section class="itinerary-page">
    <el-card class="module-card itinerary-card">
      <div class="module-header">
        <div>
          <h2>行程多人协作</h2>
          <p class="module-subtitle">支持通过行程ID共享访问、多人同时编辑并自动同步。</p>
        </div>
      </div>

      <el-divider content-position="left">创建新行程</el-divider>
      <el-form :model="createForm" label-width="120px">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="行程名称">
              <el-input v-model="createForm.name" placeholder="如：北邮校园半日游" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="创建人">
              <el-input v-model="createForm.owner" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="协作成员">
              <el-input v-model="createForm.collaborators" placeholder="逗号分隔，如：小王,小李" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="createForm.strategy" class="full-width">
                <el-option label="最短时间" value="time" />
                <el-option label="最短距离" value="distance" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交通模式">
          <el-input v-model="createForm.transportMode" placeholder="例如 walk+shuttle" />
        </el-form-item>
        <el-form-item label="行程备注">
          <el-input v-model="createForm.notes" type="textarea" :rows="2" placeholder="记录停留时长、集合地点等信息" />
        </el-form-item>
        <el-button type="primary" @click="submitCreate">创建行程</el-button>
      </el-form>

      <el-divider content-position="left">打开/编辑共享行程</el-divider>
      <div class="toolbar">
        <el-input v-model="sharedId" placeholder="输入共享行程ID" class="id-input" clearable />
        <el-button @click="openById">打开行程</el-button>
        <el-button :disabled="!selectedId" @click="refreshCurrent">刷新为最新版本</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedId" @click="submitUpdate">保存当前修改</el-button>
      </div>

      <el-form v-if="editForm" :model="editForm" label-width="120px" class="edit-form">
        <el-row :gutter="12">
          <el-col :md="8" :xs="24">
            <el-form-item label="行程ID">
              <el-input :model-value="String(editForm.id)" disabled />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="最后更新时间">
              <el-input :model-value="editForm.updatedAt || '无'" disabled />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="行程名称">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="8" :xs="24">
            <el-form-item label="创建人">
              <el-input v-model="editForm.owner" />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="协作成员">
              <el-input v-model="editForm.collaborators" />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="editForm.strategy" class="full-width">
                <el-option label="最短时间" value="time" />
                <el-option label="最短距离" value="distance" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交通模式">
          <el-input v-model="editForm.transportMode" />
        </el-form-item>
        <el-form-item label="行程备注">
          <el-input v-model="editForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">路线规划</el-divider>
      <div class="map-title">
        <span>高德地图</span>
      </div>
      <div id="itinerary-map" class="itinerary-map" />

      <el-form label-width="100px" class="route-form" style="margin-top: 16px;">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="起点">
              <el-autocomplete
                v-model="startInput"
                class="full-width"
                clearable
                placeholder="输入关键词搜索起点"
                :fetch-suggestions="fetchSuggestions"
                @select="onStartSelect"
                @input="onStartInput"
              />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="出行方式">
              <el-select v-model="transportMode" class="full-width">
                <el-option v-for="item in transportModes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="途经点">
          <div class="waypoint-list">
            <div v-for="(wp, index) in waypoints" :key="index" class="waypoint-item">
              <el-autocomplete
                v-model="wp.input"
                class="full-width"
                clearable
                :placeholder="`输入关键词搜索目的地 ${index + 1}`"
                :fetch-suggestions="fetchSuggestions"
                @select="(item) => onWaypointSelect(index, item)"
                @input="(value) => onWaypointInput(index, value)"
              />
              <el-button type="danger" plain :disabled="waypoints.length <= 1" @click="removeWaypoint(index)">删除</el-button>
              <el-button v-if="index === waypoints.length - 1" type="primary" plain @click="addWaypoint">添加</el-button>
            </div>
          </div>
        </el-form-item>
        <el-button type="primary" :loading="routeLoading" @click="planRoute">规划路线</el-button>
      </el-form>

      <el-descriptions v-if="routeResult" :column="2" border style="margin-top: 12px;">
        <el-descriptions-item label="总距离">{{ distanceKm }} km</el-descriptions-item>
        <el-descriptions-item label="预计耗时">{{ durationHours }} h</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else-if="editForm" description="请搜索起点和目的地后点击规划路线" :image-size="80" style="margin-top: 12px;" />

      <el-divider />
      <el-table :data="list" border stripe v-loading="loading" @row-click="selectItinerary">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="owner" label="创建人" width="120" />
        <el-table-column prop="collaborators" label="协作人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="strategy" label="策略" width="120" />
        <el-table-column prop="transportMode" label="交通" width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.full-width {
  width: 100%;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.id-input {
  width: 220px;
}

.edit-form {
  margin-bottom: 12px;
}

.itinerary-map {
  width: 100%;
  height: 460px;
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

.route-form {
  max-width: 900px;
}

.waypoint-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.waypoint-item {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
}
</style>
