<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { voteTypeLabel } from '../../utils/itineraryVotes'
import { wgs84ToGcj02 } from '../../utils/coordTransform'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  selectedSpotId: { type: [Number, String], default: null },
})

const emit = defineEmits(['select-node'])

const mapEl = ref(null)
let AMapApi = null
let map = null
let markers = []

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
    map.setFitView(markers, false, [64, 64, 64, 64], 16)
  }
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
    plugins: ['AMap.Scale', 'AMap.ToolBar'],
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
  renderMarkers()
}

watch(() => props.nodes, () => {
  initMap().then(renderMarkers)
}, { deep: true, immediate: true })

watch(() => props.selectedSpotId, renderMarkers)

onBeforeUnmount(() => {
  clearMarkers()
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
  min-height: 520px;
  overflow: hidden;
  border-radius: 8px;
  background: #e5e7eb;
  border: 1px solid #d7dde7;
}

.real-map-stage {
  width: 100%;
  height: 520px;
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
