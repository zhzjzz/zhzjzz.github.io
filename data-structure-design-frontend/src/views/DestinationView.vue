<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Fire, Ranking, Search, Star } from '@icon-park/vue-next'
import { getTopDestinations, searchDestinations } from '../api/travel'
import destinationDefaultImage from '../assets/defaults/destination-default.png'
import destinationCampusImage from '../assets/defaults/destination-campus-default.png'

const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const rankMode = ref('composite')
const interest = ref('')
const interestOptions = ['自然', '历史', '校园', '亲子']
const searchSort = ref('')
const isSearchMode = ref(false)

const topRows = computed(() => rows.value.slice(0, 10))
const featured = computed(() => topRows.value[0] || null)
const maxHeat = computed(() => Math.max(...topRows.value.map((item) => Number(item.heat) || 0), 1))
const hasSortableSearchResults = computed(() => isSearchMode.value && rows.value.length > 1)

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
const destinationImage = (item) => {
  const text = `${item?.sceneType || ''} ${item?.category || ''} ${item?.name || ''}`
  return /校园|大学|学院|校区/.test(text) ? destinationCampusImage : destinationDefaultImage
}

const loadTop = async () => {
  loading.value = true
  try {
    isSearchMode.value = false
    searchSort.value = ''
    const { data } = await getTopDestinations(10, rankMode.value, interest.value)
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
    const { data } = await searchDestinations(keyword.value, searchSort.value)
    rows.value = data
    isSearchMode.value = true
    ElMessage.success(`已找到 ${data.length} 条结果`)
  } finally {
    loading.value = false
  }
}

const setSearchSort = async (sort) => {
  if (searchSort.value === sort) return
  searchSort.value = sort
  await doSearch()
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
        <el-col :md="4" :xs="24">
          <el-select v-model="interest" size="large" class="full-width" clearable placeholder="兴趣" @change="loadTop">
            <el-option v-for="option in interestOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </el-col>
        <el-col :md="4" :xs="24" class="btn-group">
          <el-button type="primary" size="large" @click="doSearch">
            <Search theme="outline" size="17" fill="currentColor" />
            检索
          </el-button>
        </el-col>
      </el-row>

      <div v-if="hasSortableSearchResults" class="search-sort-bar">
        <span>查询结果排序</span>
        <el-button-group>
          <el-button :type="searchSort === 'heat' ? 'primary' : 'default'" @click="setSearchSort('heat')">热度优先</el-button>
          <el-button :type="searchSort === 'rating' ? 'primary' : 'default'" @click="setSearchSort('rating')">评分优先</el-button>
        </el-button-group>
      </div>

      <section v-if="featured" class="leaderboard-layout reveal-in">
        <article class="featured-destination">
          <img :src="destinationImage(featured)" alt="" aria-hidden="true" />
          <span class="rank-badge">Top 1</span>
          <h3>{{ featured.name }}</h3>
          <p>{{ featured.sceneType || '旅行目的地' }} · {{ featured.category || '综合推荐' }}</p>
          <div class="score-row">
            <span><Fire theme="outline" size="14" fill="currentColor" /> 热度 {{ featured.heat || 0 }}</span>
            <span><Star theme="outline" size="14" fill="currentColor" /> 评分 {{ featured.rating || '-' }}</span>
            <span>{{ modeLabel }}</span>
          </div>
        </article>

        <div class="ranking-list">
          <article v-for="(item, index) in topRows" :key="item.id || item.name" class="ranking-item">
            <div class="ranking-index">
              <Ranking theme="outline" size="18" fill="currentColor" />
              {{ index + 1 }}
            </div>
            <div class="ranking-main">
              <strong>{{ item.name }}</strong>
              <span>{{ item.sceneType || '目的地' }} · {{ item.category || '未分类' }}</span>
              <div class="bar-line">
                <i :style="{ width: `${heatPercent(item)}%` }" />
              </div>
            </div>
            <div class="ranking-score">
              <span><Fire theme="outline" size="14" fill="currentColor" /> 热度 {{ item.heat || 0 }}</span>
              <span><Star theme="outline" size="14" fill="currentColor" /> 评分 {{ item.rating || '-' }} · {{ ratingPercent(item) }}%</span>
            </div>
          </article>
        </div>
      </section>

      <el-empty v-else-if="!loading" description="暂无推荐数据，请检查后端服务或搜索条件" />

        <div class="destination-results" v-loading="loading">
        <div class="destination-row destination-row-head">
          <span></span>
          <span>名称</span>
          <span>场景</span>
          <span>类别</span>
          <span>热度</span>
          <span>评分</span>
        </div>
        <article v-for="(item, index) in rows" :key="item.id || `${item.name}-${index}`" class="destination-row">
          <img class="destination-thumb" :src="destinationImage(item)" :alt="`${item.name || '目的地'}默认图`" loading="lazy" />
          <div class="destination-name">
            <strong>{{ item.name }}</strong>
            <small>{{ item.sceneType || '目的地' }} · {{ item.category || '未分类' }}</small>
          </div>
          <span class="destination-chip">{{ item.sceneType || '目的地' }}</span>
          <span class="destination-category">{{ item.category || '-' }}</span>
          <strong class="destination-heat">{{ item.heat || 0 }}</strong>
          <strong class="destination-rating">{{ item.rating || '-' }}</strong>
        </article>
      </div>
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

.search-sort-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin: -4px 0 16px;
}

.search-sort-bar span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.leaderboard-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
  margin-bottom: 18px;
}

.featured-destination {
  position: relative;
  overflow: hidden;
  min-height: 260px;
  padding: 24px;
  border-radius: 22px;
  color: #ffffff;
  background: #111827;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
}

.featured-destination::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(135deg, rgba(17, 24, 39, 0.9), rgba(255, 56, 92, 0.6));
}

.featured-destination > *:not(img) {
  position: relative;
  z-index: 2;
}

.featured-destination img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.destination-results {
  margin-top: 18px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.2);
}

.destination-row {
  display: grid;
  grid-template-columns: 74px minmax(220px, 1.7fr) minmax(110px, 0.55fr) minmax(130px, 0.7fr) 100px 90px;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #d7dce5;
}

.destination-thumb {
  width: 74px;
  height: 54px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.destination-row:nth-child(odd):not(.destination-row-head) {
  background: rgba(255, 255, 255, 0.035);
}

.destination-row:hover:not(.destination-row-head) {
  background: rgba(255, 56, 92, 0.08);
}

.destination-row-head {
  border-top: 0;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  font-size: 13px;
  font-weight: 900;
}

.destination-name strong,
.destination-name small {
  display: block;
}

.destination-name strong {
  color: #f8fafc;
  line-height: 1.4;
}

.destination-name small,
.destination-category {
  color: #a7b0bf;
}

.destination-chip {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(243, 208, 138, 0.12);
  color: #f3d08a;
  font-size: 12px;
  font-weight: 800;
}

.destination-heat,
.destination-rating {
  justify-self: end;
  color: #f8fafc;
  font-size: 16px;
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

  .destination-row,
  .destination-row-head {
    grid-template-columns: 1fr;
  }

  .destination-row-head {
    display: none;
  }

  .destination-heat,
  .destination-rating {
    justify-self: start;
  }
}
</style>
