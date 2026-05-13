<script setup>
import { computed } from 'vue'
import { voteTypeLabel } from '../../utils/itineraryVotes'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  selectedSpotId: { type: [Number, String], default: null },
})

const emit = defineEmits(['select-node'])

const lines = computed(() => props.nodes.slice(1).map((node, index) => ({
  key: `${props.nodes[index].spotId}-${node.spotId}`,
  x1: props.nodes[index].x,
  y1: props.nodes[index].y,
  x2: node.x,
  y2: node.y,
})))

const nodeClass = (node) => [
  'tactical-node',
  `node-${node.consensus || 'backup'}`,
  { active: String(node.spotId) === String(props.selectedSpotId) },
]
</script>

<template>
  <section class="tactical-map-panel">
    <div class="map-copy">
      <span class="map-eyebrow">协作战术地图</span>
      <h3>像游戏 ping 地图一样共同选择景点</h3>
      <p>点击节点查看队友投票，并快速标记必去、想去、不想去或备选。</p>
    </div>
    <div class="map-stage">
      <svg class="route-lines" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
        <line
          v-for="line in lines"
          :key="line.key"
          :x1="line.x1"
          :y1="line.y1"
          :x2="line.x2"
          :y2="line.y2"
        />
      </svg>
      <button
        v-for="node in nodes"
        :key="node.spotId"
        type="button"
        :class="nodeClass(node)"
        :style="{ left: `${node.x}%`, top: `${node.y}%` }"
        @click="emit('select-node', node)"
      >
        <strong>{{ node.spotName }}</strong>
        <span>{{ voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus) }}</span>
        <small>{{ node.votes.length }} 票</small>
      </button>
      <div v-if="!nodes.length" class="empty-map">暂无投票节点，先为一个景点发起投票。</div>
    </div>
  </section>
</template>

<style scoped>
.tactical-map-panel {
  display: grid;
  grid-template-columns: minmax(220px, 0.42fr) minmax(0, 1fr);
  gap: 18px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #11151c, #1d2b3a);
  color: #f8fafc;
}

.map-eyebrow {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 56, 92, 0.16);
  color: #ff8ba0;
  font-size: 12px;
  font-weight: 900;
}

.map-copy h3 {
  margin: 12px 0 8px;
  font-size: 24px;
}

.map-copy p {
  color: #a7b0bf;
  line-height: 1.7;
}

.map-stage {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  border-radius: 22px;
  background:
    radial-gradient(circle at 18% 28%, rgba(255, 56, 92, 0.18), transparent 18%),
    radial-gradient(circle at 70% 72%, rgba(243, 208, 138, 0.18), transparent 20%),
    linear-gradient(135deg, #e6f5ed, #fff4d9);
}

.route-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.route-lines line {
  stroke: rgba(34, 34, 34, 0.42);
  stroke-width: 1.4;
  stroke-linecap: round;
}

.tactical-node {
  position: absolute;
  transform: translate(-50%, -50%);
  min-width: 108px;
  border: 3px solid #ffffff;
  border-radius: 18px;
  padding: 10px 12px;
  color: #ffffff;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.22);
  cursor: pointer;
}

.tactical-node strong,
.tactical-node span,
.tactical-node small {
  display: block;
}

.tactical-node strong {
  font-size: 14px;
}

.tactical-node span,
.tactical-node small {
  margin-top: 3px;
  font-size: 12px;
}

.node-must {
  background: #ff385c;
}

.node-want {
  background: #0f766e;
}

.node-conflict {
  background: #f59e0b;
}

.node-backup {
  background: #64748b;
}

.tactical-node.active {
  outline: 4px solid rgba(255, 255, 255, 0.66);
}

.empty-map {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #475569;
  font-weight: 800;
}

@media (max-width: 900px) {
  .tactical-map-panel {
    grid-template-columns: 1fr;
  }
}
</style>
