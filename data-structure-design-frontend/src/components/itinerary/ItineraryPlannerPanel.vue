<script setup>
import { computed, ref, watch } from 'vue'
import { Calendar, Down, PlayOne, Up } from '@icon-park/vue-next'
import { useItineraryPlanner } from '../../composables/useItineraryPlanner'
import {
  defaultPlannerSelection,
  formatDateTime,
  formatDuration,
  formatKm,
  selectedPlannerCount,
} from '../../utils/itineraryPlanner'

const props = defineProps({
  spots: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['planned'])

const plannerSpots = ref([])
const departureTime = ref('')
const strategy = ref('SHORTEST_TIME')
const optimizeVisitOrder = ref(true)

const { loading, preview, error, generatePreview, resetPreview } = useItineraryPlanner()

const selectedCount = computed(() => selectedPlannerCount(plannerSpots.value))
const canPlan = computed(() => selectedCount.value >= 1)

watch(() => props.spots, (spots) => {
  plannerSpots.value = defaultPlannerSelection(spots)
  resetPreview()
}, { immediate: true, deep: true })

const moveSpot = (index, delta) => {
  const next = index + delta
  if (next < 0 || next >= plannerSpots.value.length) return
  const copy = [...plannerSpots.value]
  const [item] = copy.splice(index, 1)
  copy.splice(next, 0, item)
  plannerSpots.value = copy.map((spot, orderIndex) => ({ ...spot, orderIndex }))
}

const submit = async () => {
  const result = await generatePreview({
    departureTime: departureTime.value,
    strategy: strategy.value,
    optimizeVisitOrder: optimizeVisitOrder.value,
    spots: plannerSpots.value,
  })
  if (result) emit('planned', result)
}
</script>

<template>
  <section class="planner-panel">
    <header class="planner-head">
      <div>
        <strong>一键规划</strong>
        <span>{{ selectedCount }} / {{ plannerSpots.length }} 个景点</span>
      </div>
      <el-button type="primary" :loading="loading" :disabled="!canPlan" @click="submit">
        <PlayOne theme="outline" size="16" fill="currentColor" />
        生成
      </el-button>
    </header>

    <div class="planner-controls">
      <el-date-picker
        v-model="departureTime"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="出发时间"
        class="planner-date"
      />
      <el-select v-model="strategy" class="planner-select">
        <el-option label="最短时间" value="SHORTEST_TIME" />
        <el-option label="最短距离" value="SHORTEST_DISTANCE" />
      </el-select>
      <el-switch v-model="optimizeVisitOrder" active-text="自动排序" inactive-text="手动顺序" />
    </div>

    <div class="planner-spots">
      <article v-for="(spot, index) in plannerSpots" :key="spot.spotId || `${spot.spotName}-${index}`" class="planner-spot">
        <el-checkbox v-model="spot.selected" />
        <div>
          <strong>{{ spot.spotName }}</strong>
          <span>{{ spot.consensus || 'backup' }}</span>
        </div>
        <div class="stay-control">
          <small>停留</small>
          <el-input-number
            v-model="spot.stayMinutes"
            :min="0"
            :max="720"
            :step="15"
            size="small"
            controls-position="right"
          />
          <small>分钟</small>
        </div>
        <div class="spot-actions">
          <el-button text :disabled="index === 0" @click="moveSpot(index, -1)">
            <Up theme="outline" size="14" fill="currentColor" />
          </el-button>
          <el-button text :disabled="index === plannerSpots.length - 1" @click="moveSpot(index, 1)">
            <Down theme="outline" size="14" fill="currentColor" />
          </el-button>
        </div>
      </article>
    </div>

    <el-alert v-if="error" type="warning" :title="error" show-icon :closable="false" />

    <section v-if="preview" class="planner-preview">
      <div class="preview-metrics">
        <article>
          <span>总距离</span>
          <strong>{{ formatKm(preview.totalDistance) }}</strong>
        </article>
        <article>
          <span>总耗时</span>
          <strong>{{ formatDuration(preview.totalTime) }}</strong>
        </article>
        <article>
          <span><Calendar theme="outline" size="14" fill="currentColor" /> 抵达</span>
          <strong>{{ formatDateTime(preview.arrivalTime) }}</strong>
        </article>
      </div>

      <div v-if="preview.warnings?.length" class="planner-warnings">
        <el-tag v-for="warning in preview.warnings" :key="warning" type="warning" effect="plain">
          {{ warning }}
        </el-tag>
      </div>

      <div class="timeline-list">
        <article v-for="(item, index) in preview.timeline || []" :key="`${item.label}-${index}`" class="timeline-item">
          <strong>{{ index + 1 }}. {{ item.label }}</strong>
          <span>{{ formatDuration(item.duration) }} · {{ formatKm(item.distance) }}</span>
          <small v-if="item.startTime || item.endTime">
            {{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}
          </small>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.planner-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.planner-head,
.planner-controls,
.planner-spot,
.spot-actions,
.preview-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
}

.planner-head {
  justify-content: space-between;
}

.planner-head strong,
.planner-head span {
  display: block;
}

.planner-head strong {
  color: #111827;
  font-size: 15px;
  font-weight: 900;
}

.planner-head span,
.planner-spot span,
.timeline-item span,
.timeline-item small {
  color: #64748b;
  font-size: 12px;
}

.planner-controls {
  flex-wrap: wrap;
}

.planner-date {
  max-width: 210px;
}

.planner-select {
  max-width: 140px;
}

.planner-spots,
.planner-preview,
.timeline-list {
  display: grid;
  gap: 8px;
}

.planner-spot {
  justify-content: space-between;
  padding: 9px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.planner-spot > div:nth-child(2) {
  flex: 1;
  min-width: 0;
}

.stay-control {
  display: grid;
  grid-template-columns: auto 92px auto;
  align-items: center;
  gap: 6px;
}

.stay-control small {
  color: #64748b;
  font-size: 12px;
}

.planner-spot strong {
  display: block;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spot-actions {
  gap: 2px;
}

.planner-preview {
  padding-top: 4px;
}

.preview-metrics {
  align-items: stretch;
  flex-wrap: wrap;
}

.preview-metrics article {
  flex: 1 1 120px;
  padding: 10px;
  border-radius: 8px;
  background: #0f172a;
  color: #ffffff;
}

.preview-metrics span {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #cbd5e1;
  font-size: 12px;
}

.preview-metrics strong {
  display: block;
  margin-top: 4px;
  font-size: 15px;
}

.planner-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.timeline-item {
  display: grid;
  gap: 3px;
  padding: 10px 12px;
  border-left: 3px solid #2563eb;
  border-radius: 8px;
  background: #eff6ff;
}

.timeline-item strong {
  color: #1e3a8a;
  font-size: 13px;
}

@media (max-width: 720px) {
  .planner-head,
  .planner-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .planner-date,
  .planner-select {
    max-width: none;
    width: 100%;
  }
}
</style>
