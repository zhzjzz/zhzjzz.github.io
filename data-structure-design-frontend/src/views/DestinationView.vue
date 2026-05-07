<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTopDestinations, searchDestinations } from '../api/travel'

const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const rankMode = ref('composite')

const topRows = computed(() => rows.value.slice(0, 10))
const featured = computed(() => topRows.value[0] || null)
const maxHeat = computed(() => Math.max(...topRows.value.map((item) => Number(item.heat) || 0), 1))

const modeLabel = computed(() => {
  const labels = {
    composite: '综合推荐',
    rating: '评分 Top10',
    heat: '热度 Top10',
  }
  return labels[rankMode.value] || '综合推荐'
})

const heatPercent = (item) => Math.round(((Number(item.heat) || 0) / maxHeat.value) * 100)
const ratingPercent = (item) => Math.min(100, Math.round(((Number(item.rating) || 0) / 5) * 100))

const loadTop = async () => {
  loading.value = true
  try {
    const { data } = await getTopDestinations(10, rankMode.value)
    rows.value = data
  } finally {
    loading.value = false
  }
}

const doSearch = async () => {
  if (!keyword.value.trim()) {
    await loadTop()
    return
  }
  loading.value = true
  try {
    const { data } = await searchDestinations(keyword.value)
    rows.value = data
    ElMessage.success(`已找到 ${data.length} 条结果`)
  } finally {
    loading.value = false
  }
}

onMounted(loadTop)
</script>

<template>
  <section class="destination-page">
    <el-card class="module-card destination-card">
      <div class="module-header">
        <div>
          <p class="demo-eyebrow">Recommendation</p>
          <h2>目的地推荐</h2>
          <p class="module-subtitle">支持景区/校园目的地 Top-K 推荐、关键词检索和多维排序展示。</p>
        </div>
      </div>

      <el-row :gutter="12" class="toolbar-row">
        <el-col :md="12" :xs="24">
          <el-input
            v-model="keyword"
            placeholder="输入关键词（名称/类别）"
            clearable
            size="large"
            @keyup.enter="doSearch"
          />
        </el-col>
        <el-col :md="6" :xs="24">
          <el-select v-model="rankMode" size="large" class="full-width" @change="loadTop">
            <el-option label="综合推荐" value="composite" />
            <el-option label="评分 Top10" value="rating" />
            <el-option label="热度 Top10" value="heat" />
          </el-select>
        </el-col>
        <el-col :md="6" :xs="24" class="btn-group">
          <el-button type="primary" size="large" @click="doSearch">检索</el-button>
          <el-button size="large" @click="loadTop">Top10</el-button>
        </el-col>
      </el-row>

      <section v-if="featured" class="leaderboard-layout reveal-in">
        <article class="featured-destination">
          <span class="rank-badge">Top 1</span>
          <h3>{{ featured.name }}</h3>
          <p>{{ featured.sceneType || '旅行目的地' }} · {{ featured.category || '综合推荐' }}</p>
          <div class="score-row">
            <span>热度 {{ featured.heat || 0 }}</span>
            <span>评分 {{ featured.rating || '-' }}</span>
            <span>{{ modeLabel }}</span>
          </div>
        </article>

        <div class="ranking-list">
          <article v-for="(item, index) in topRows" :key="item.id || item.name" class="ranking-item">
            <div class="ranking-index">{{ index + 1 }}</div>
            <div class="ranking-main">
              <strong>{{ item.name }}</strong>
              <span>{{ item.sceneType || '目的地' }} · {{ item.category || '未分类' }}</span>
              <div class="bar-line">
                <i :style="{ width: `${heatPercent(item)}%` }" />
              </div>
            </div>
            <div class="ranking-score">
              <span>热度 {{ item.heat || 0 }}</span>
              <span>评分 {{ item.rating || '-' }} · {{ ratingPercent(item) }}%</span>
            </div>
          </article>
        </div>
      </section>

      <el-empty v-else-if="!loading" description="暂无推荐数据，请检查后端服务或搜索条件" />

      <el-table :data="rows" stripe border v-loading="loading" class="detail-table">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="sceneType" label="场景" width="120" />
        <el-table-column prop="category" label="类别" width="160" />
        <el-table-column prop="heat" label="热度" width="120" />
        <el-table-column prop="rating" label="评分" width="120" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.toolbar-row {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.btn-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.leaderboard-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
  margin-bottom: 18px;
}

.featured-destination {
  min-height: 260px;
  padding: 24px;
  border-radius: 22px;
  color: #ffffff;
  background: linear-gradient(135deg, #111827 0%, #ff385c 100%);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
}

.rank-badge {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 900;
}

.featured-destination h3 {
  margin-top: 22px;
  font-size: 30px;
  line-height: 1.18;
  font-weight: 900;
}

.featured-destination p {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 500;
}

.score-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}

.score-row span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.ranking-list {
  display: grid;
  gap: 10px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.ranking-index {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #fff1f4;
  color: #ff385c;
  font-weight: 900;
}

.ranking-main strong,
.ranking-main span,
.ranking-score span {
  display: block;
}

.ranking-main strong {
  color: #111827;
  font-size: 16px;
}

.ranking-main span,
.ranking-score span {
  color: #64748b;
  font-size: 12px;
}

.bar-line {
  height: 6px;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.bar-line i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff385c, #d97706);
}

.detail-table {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .btn-group {
    justify-content: flex-start;
    margin-top: 8px;
  }

  .leaderboard-layout,
  .ranking-item {
    grid-template-columns: 1fr;
  }

  .ranking-score {
    display: flex;
    gap: 12px;
  }
}
</style>
