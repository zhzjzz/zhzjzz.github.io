<script setup>
import { computed } from 'vue'
import { summarizeConsensus } from '../../utils/itineraryVotes'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
})

const summary = computed(() => summarizeConsensus(props.nodes))
const percent = computed(() => summary.value.total ? Math.round((summary.value.agreed / summary.value.total) * 100) : 0)
</script>

<template>
  <section class="consensus-progress">
    <div>
      <span>团队共识进度</span>
      <strong>{{ summary.agreed }} / {{ summary.total }}</strong>
      <small>{{ summary.conflicts }} 个景点存在分歧，{{ summary.must }} 个景点被标记为必去</small>
    </div>
    <el-progress type="circle" :percentage="percent" :width="86" color="#ff385c" />
  </section>
</template>

<style scoped>
.consensus-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff8f8, #fff1f4);
  border: 1px solid #ffd8e1;
}

.consensus-progress span,
.consensus-progress strong,
.consensus-progress small {
  display: block;
}

.consensus-progress span {
  color: #ff385c;
  font-size: 12px;
  font-weight: 900;
}

.consensus-progress strong {
  margin-top: 4px;
  color: #222222;
  font-size: 28px;
}

.consensus-progress small {
  margin-top: 4px;
  color: #64748b;
}
</style>
