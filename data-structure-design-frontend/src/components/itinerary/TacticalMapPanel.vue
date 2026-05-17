<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { voteTypeLabel } from '../../utils/itineraryVotes'
import { wgs84ToGcj02, wgs84ToGcj02Batch } from '../../utils/coordTransform'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  selectedSpotId: { type: [Number, String], default: null },
  routeSegments: { type: Array, default: () => [] },
})

const emit = defineEmits(['select-node'])

const mapEl = ref(null)
let AMapApi = null
let map = null
let markers = []
let routeLayers = []
let drivingLayers = []
let resizeObserver = null
const routeColors = ['#ff385c', '#176b5d', '#f59e0b', '#2563eb', '#7c3aed', '#111827']

const consensusColor = (consensus) => ({
  must: '#ff385c',
  want: '#0f766e',
  conflict: '#f59e0b',
  backup: '#64748b',
}[consensus] || '#64748b')

const clearMarkers = () => {
  if (map && markers.length) {
    map.remove(markers)
  }
  markers = []
}

const clearRoutes = () => {
  if (map && routeLayers.length) {
    map.remove(routeLayers)
  }
  routeLayers = []
  drivingLayers.forEach((driving) => driving?.clear?.())
  drivingLayers = []
}

const refreshMapView = () => {
  if (!map) return
  map.resize?.()
  const overlays = [...markers, ...routeLayers].filter(Boolean)
  if (overlays.length) {
    map.setFitView(overlays, false, [12, 12, 12, 12], 18)
  }
}

const scheduleMapResize = () => {
  ;[0, 80, 220, 480, 900].forEach((delay) => {
    setTimeout(refreshMapView, delay)
  })
}

const markerContent = (node) => `
  <button class="itinerary-map-marker ${String(node.spotId) === String(props.selectedSpotId) ? 'active' : ''}" type="button">
    <span class="marker-dot" style="background:${consensusColor(node.consensus)}"></span>
    <strong>${node.spotName}</strong>
    <small>${voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus)} · ${node.votes.length}票</small>
  </button>
`

const renderMarkers = () => {
  if (!map || !AMapApi) return
  clearMarkers()
  markers = props.nodes.map((node) => {
    const [lng, lat] = wgs84ToGcj02(node.longitude, node.latitude)
    const marker = new AMapApi.Marker({
      position: [lng, lat],
      anchor: 'bottom-center',
      content: markerContent(node),
      zIndex: String(node.spotId) === String(props.selectedSpotId) ? 120 : 100,
    })
    marker.on('click', () => emit('select-node', node))
    return marker
  })
  if (markers.length) {
    map.add(markers)
    refreshMapView()
  }
}

const drawMicroPath = (coords, color, dashed = false) => {
  if (!map || !AMapApi || !coords?.length) return null
  const gcjPath = wgs84ToGcj02Batch(coords)
  const amapPath = gcjPath.map(([lat, lng]) => [lng, lat])
  const polyline = new AMapApi.Polyline({
    path: amapPath,
    strokeColor: color,
    strokeWeight: dashed ? 6 : 7,
    strokeOpacity: 0.94,
    strokeStyle: dashed ? 'dashed' : 'solid',
    isOutline: true,
    outlineColor: '#ffffff',
    lineJoin: 'round',
    showDir: true,
    zIndex: 90,
  })
  map.add(polyline)
  return polyline
}

const drawCityLink = (startCoord, endCoord) => {
  if (!map || !AMapApi || !startCoord || !endCoord) return null
  const [startLat, startLng] = startCoord
  const [endLat, endLng] = endCoord
  const [gcjStartLng, gcjStartLat] = wgs84ToGcj02(startLng, startLat)
  const [gcjEndLng, gcjEndLat] = wgs84ToGcj02(endLng, endLat)
  const driving = new AMapApi.Driving({
    map,
    hideMarkers: true,
  })
  driving.search(
    new AMapApi.LngLat(gcjStartLng, gcjStartLat),
    new AMapApi.LngLat(gcjEndLng, gcjEndLat)
  )
  drivingLayers.push(driving)
  return driving
}

const renderRoutes = () => {
  if (!map || !AMapApi) return
  clearRoutes()
  props.routeSegments.forEach((segment, index) => {
    const color = routeColors[index % routeColors.length]
    if (segment.type === 'city') {
      drawCityLink(segment.cityTransitStart, segment.cityTransitEnd)
      return
    }
    if (segment.path?.length) {
      const line = drawMicroPath(segment.path, color)
      if (line) routeLayers.push(line)
    }
  })
  refreshMapView()
}

const initMap = async () => {
  await nextTick()
  if (map || !mapEl.value) return
  const amapKey = (import.meta.env.VITE_AMAP_KEY || '').trim()
  if (!amapKey) {
    ElMessage.warning('未配置 VITE_AMAP_KEY，协作地图无法加载')
    return
  }
  const amapSecret = (import.meta.env.VITE_AMAP_SECRET || '').trim()
  if (amapSecret) {
    window._AMapSecurityConfig = { securityJsCode: amapSecret }
  }
  AMapApi = await AMapLoader.load({
    key: amapKey,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Driving'],
  })
  map = new AMapApi.Map(mapEl.value, {
    zoom: 4,
    center: [104.1954, 35.8617],
    mapStyle: 'amap://styles/normal',
    viewMode: '2D',
    features: ['bg', 'road', 'point'],
  })
  map.addControl(new AMapApi.Scale())
  map.addControl(new AMapApi.ToolBar({ position: 'RB' }))
  resizeObserver = new ResizeObserver(() => requestAnimationFrame(refreshMapView))
  resizeObserver.observe(mapEl.value)
  renderMarkers()
  renderRoutes()
  scheduleMapResize()
}

watch(() => props.nodes, () => {
  initMap().then(() => {
    renderMarkers()
    scheduleMapResize()
  })
}, { deep: true, immediate: true })

watch(() => props.selectedSpotId, () => {
  renderMarkers()
  scheduleMapResize()
})

watch(() => props.routeSegments, () => {
  initMap().then(() => {
    renderRoutes()
    scheduleMapResize()
  })
}, { deep: true, immediate: true })

onBeforeUnmount(() => {
  clearRoutes()
  clearMarkers()
  resizeObserver?.disconnect?.()
  resizeObserver = null
  map?.destroy?.()
  map = null
  AMapApi = null
})
</script>

<template>
  <section class="tactical-map-panel">
    <div ref="mapEl" class="real-map-stage"></div>
    <div v-if="!nodes.length" class="empty-map">先添加真实景点</div>
  </section>
</template>

<style scoped>
.tactical-map-panel {
  position: relative;
  height: clamp(420px, 56vh, 610px);
  min-height: 420px;
  overflow: hidden;
  border-radius: 8px;
  background: #dbeafe;
  border: 1px solid rgba(56, 189, 248, 0.26);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
}

.real-map-stage {
  width: 100%;
  height: 100%;
  min-height: 100%;
  background: #dbeafe;
}

.real-map-stage :global(.amap-container),
.real-map-stage :global(.amap-maps),
.real-map-stage :global(.amap-layers),
.real-map-stage :global(.amap-layer),
.real-map-stage :global(.amap-tile-container),
.real-map-stage :global(.amap-tile) {
  width: 100% !important;
  height: 100% !important;
}

.empty-map {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.72);
  color: #475569;
  font-weight: 800;
  pointer-events: none;
}

:global(.itinerary-map-marker) {
  display: grid;
  min-width: 132px;
  gap: 2px;
  padding: 8px 10px;
  border: 2px solid #ffffff;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.26);
  cursor: pointer;
  text-align: left;
}

:global(.itinerary-map-marker.active) {
  outline: 3px solid rgba(255, 56, 92, 0.42);
}

:global(.itinerary-map-marker strong) {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

:global(.itinerary-map-marker small) {
  color: #d7dce5;
  font-size: 11px;
}

:global(.marker-dot) {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

@media (max-width: 900px) {
  .tactical-map-panel,
  .real-map-stage {
    min-height: 420px;
    height: 420px;
  }
}
</style>
