<script setup>
defineProps({
  keyword: {
    type: String,
    default: '',
  },
  results: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  addingId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(['update:keyword', 'search', 'add'])

const updateKeyword = (value) => {
  emit('update:keyword', value)
}

const submitSearch = () => {
  emit('search')
}

const addSpot = (spot) => {
  emit('add', spot)
}
</script>

<template>
  <section class="real-spot-card">
    <div class="real-spot-head">
      <div>
        <strong>添加景点</strong>
      </div>
    </div>

    <el-input
      :model-value="keyword"
      clearable
      placeholder="搜索景点名称"
      @update:model-value="updateKeyword"
      @keyup.enter="submitSearch"
      @clear="submitSearch"
    >
      <template #append>
        <el-button :loading="loading" @click="submitSearch">搜索</el-button>
      </template>
    </el-input>

    <div v-loading="loading" class="real-spot-results">
      <div v-if="!results.length" class="empty-tip">输入关键词后选择真实景点。</div>
      <article v-for="spot in results" :key="spot.id" class="real-spot-row">
        <div>
          <strong>{{ spot.name || spot.spotName || `景点 ${spot.id}` }}</strong>
          <span>{{ spot.city || spot.province || '目的地库' }}</span>
          <small v-if="spot.latitude && spot.longitude">
            {{ Number(spot.latitude).toFixed(5) }}, {{ Number(spot.longitude).toFixed(5) }}
          </small>
          <small v-else class="warn">缺少坐标，不能加入地图</small>
        </div>
        <el-button
          size="small"
          type="primary"
          :loading="String(addingId) === String(spot.id)"
          :disabled="!spot.latitude || !spot.longitude"
          @click="addSpot(spot)"
        >
          加入
        </el-button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.real-spot-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgba(255, 95, 123, 0.12), transparent 34%),
    #ffffff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
}

.real-spot-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.real-spot-head strong,
.real-spot-head span {
  display: block;
}

.real-spot-head strong {
  color: #111827;
  font-size: 15px;
  font-weight: 900;
}

.real-spot-head span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.real-spot-results {
  display: grid;
  gap: 8px;
  min-height: 44px;
}

.real-spot-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.86);
}

.real-spot-row strong,
.real-spot-row span,
.real-spot-row small {
  display: block;
}

.real-spot-row strong {
  color: #172033;
  font-size: 13px;
  line-height: 1.4;
}

.real-spot-row span,
.real-spot-row small,
.empty-tip {
  color: #64748b;
  font-size: 12px;
}

.real-spot-row small {
  margin-top: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.warn {
  color: #d97706 !important;
}

.empty-tip {
  padding: 8px 2px;
}
</style>
