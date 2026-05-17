<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bowl, Fire, Refresh, Search } from '@icon-park/vue-next'
import { listDestinations, listFoodCuisines, searchFoods } from '../api/travel'
import foodDefaultImage from '../assets/defaults/food-default.png'

const loading = ref(false)
const optionLoading = ref(false)
const foods = ref([])
const destinations = ref([])
const cuisines = ref([])

const form = ref({
  keyword: '',
  cuisine: '',
  destinationId: null,
  sort: 'recommend',
  limit: 30,
})

const resultTitle = computed(() => `${foods.value.length} 条美食推荐`)

const sortOptions = [
  { label: '综合推荐', value: 'recommend' },
  { label: '评分优先', value: 'rating' },
  { label: '目的地热度', value: 'destinationHeat' },
]

const loadOptions = async () => {
  optionLoading.value = true
  try {
    const [destinationResponse, cuisineResponse] = await Promise.all([listDestinations(), listFoodCuisines()])
    destinations.value = Array.isArray(destinationResponse.data) ? destinationResponse.data : []
    cuisines.value = Array.isArray(cuisineResponse.data) ? cuisineResponse.data.filter(Boolean) : []
  } catch (error) {
    console.error(error)
    ElMessage.warning('筛选项加载失败，仍可直接搜索美食')
  } finally {
    optionLoading.value = false
  }
}

const search = async () => {
  loading.value = true
  try {
    const params = {
      keyword: form.value.keyword.trim() || undefined,
      cuisine: form.value.cuisine || undefined,
      destinationId: form.value.destinationId || undefined,
      sort: form.value.sort,
      limit: form.value.limit,
    }
    const { data } = await searchFoods(params)
    foods.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error(error)
    foods.value = []
    ElMessage.error('美食数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const reset = async () => {
  form.value = {
    keyword: '',
    cuisine: '',
    destinationId: null,
    sort: 'recommend',
    limit: 30,
  }
  await search()
}

const destinationName = (item) => item.destination?.name || '附近目的地'

const ratingText = (item) => {
  const rating = Number(item?.rating)
  if (!Number.isFinite(rating) || rating <= 0) return '-'
  return rating.toFixed(1)
}

const heatText = (item) => Math.round(item?.heat || 0)

const foodExcerpt = (item) => {
  const storeName = String(item?.storeName || '').trim()
  if (!storeName) return '为旅途补一口好味道。'
  return `${storeName} · ${destinationName(item)}`
}

const foodTags = (item) => {
  const tags = []
  if (item?.cuisine) tags.push(item.cuisine)
  if (item?.destination?.name) tags.push(item.destination.name)
  if (item?.sourceType) tags.push(item.sourceType)
  return tags.length ? tags : ['本地风味']
}

const foodCardRatio = (index) => {
  const ratios = ['1 / 1', '4 / 5', '5 / 4', '3 / 4']
  return ratios[index % ratios.length]
}

const foodRankText = (item) => {
  const rating = ratingText(item)
  return rating === '-' ? '热度推荐' : `${rating} 分`
}

onMounted(async () => {
  await loadOptions()
  await search()
})
</script>

<template>
  <section class="food-page">
    <section class="food-hero reveal-in">
      <div class="hero-copy">
        <p class="demo-eyebrow">Local Taste</p>
        <h2>把美食结果做成更像小红书的图文种草流</h2>
        <p class="module-subtitle">保留按关键字、菜系和目的地筛选，但把结果改成图片优先的卡片流。</p>
      </div>
      <div class="food-count">
        <Bowl theme="outline" size="20" fill="currentColor" />
        <strong>{{ resultTitle }}</strong>
      </div>
    </section>

    <section class="search-board reveal-in">
      <el-form :model="form" label-width="78px" @submit.prevent>
        <el-row :gutter="12">
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="关键词">
              <el-input
                v-model="form.keyword"
                clearable
                placeholder="美食名 / 店名 / 目的地"
                @keyup.enter="search"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="菜系">
              <el-select
                v-model="form.cuisine"
                class="full-width"
                clearable
                filterable
                :loading="optionLoading"
                placeholder="全部菜系"
              >
                <el-option v-for="item in cuisines" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="目的地">
              <el-select
                v-model="form.destinationId"
                class="full-width"
                clearable
                filterable
                :loading="optionLoading"
                placeholder="全部目的地"
              >
                <el-option
                  v-for="destination in destinations"
                  :key="destination.id"
                  :label="destination.name"
                  :value="destination.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="排序">
              <el-select v-model="form.sort" class="full-width">
                <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="2" :md="4" :xs="24">
            <el-form-item label="数量">
              <el-input-number v-model="form.limit" :min="6" :max="60" :step="6" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="toolbar">
          <el-tag type="danger" effect="plain">推荐策略：评分 + 热度 + 目的地热度</el-tag>
          <div class="toolbar-actions">
            <el-button @click="reset">
              <Refresh theme="outline" size="16" fill="currentColor" />
              重置
            </el-button>
            <el-button type="primary" :loading="loading" @click="search">
              <Search theme="outline" size="16" fill="currentColor" />
              搜索美食
            </el-button>
          </div>
        </div>
      </el-form>
    </section>

    <div v-if="!foods.length && !loading" class="food-empty">
      <el-empty description="暂无匹配美食" />
    </div>

    <div v-else class="food-grid" v-loading="loading">
      <article
        v-for="(item, index) in foods"
        :key="item.id || `${item.name}-${item.storeName}`"
        class="food-card reveal-in"
      >
        <div class="food-media" :style="{ '--card-ratio': foodCardRatio(index) }">
          <img :src="item.imageUrl || foodDefaultImage" :alt="item.name || '美食图片'" loading="lazy" />
          <div class="food-media__overlay">
            <span class="food-rank">{{ foodRankText(item) }}</span>
            <span class="food-hot">
              <Fire theme="outline" size="14" fill="currentColor" />
              {{ heatText(item) }}
            </span>
          </div>
        </div>
        <div class="food-body">
          <div class="food-topline">
            <span class="food-cuisine">{{ item.cuisine || '地方风味' }}</span>
            <span class="food-destination">{{ destinationName(item) }}</span>
          </div>
          <h3>{{ item.name }}</h3>
          <p>{{ foodExcerpt(item) }}</p>
          <div class="food-tags">
            <span v-for="tag in foodTags(item)" :key="tag" class="food-chip">{{ tag }}</span>
          </div>
          <div class="food-stats">
            <span>评分 {{ ratingText(item) }}</span>
            <span>热度 {{ heatText(item) }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.food-page {
  --el-color-primary: #ff385c;
  --el-fill-color-blank: #17191d;
  --el-text-color-primary: #f8fafc;
  --el-text-color-regular: #a7b0bf;
  --el-border-color: rgba(255, 255, 255, 0.12);
  --el-border-radius-base: 8px;
  color: #f8fafc;
  display: grid;
  gap: 18px;
  background:
    linear-gradient(135deg, rgba(255, 56, 92, 0.11), transparent 30%),
    linear-gradient(180deg, rgba(23, 25, 29, 0.96), #0d0f12 56%);
}

.food-hero,
.search-board,
.food-card,
.food-empty {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(23, 25, 29, 0.86);
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(18px);
}

.food-hero::before,
.search-board::before,
.food-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(120deg, transparent 8%, rgba(255, 255, 255, 0.16) 18%, transparent 30%);
  opacity: 0;
  transform: translateX(-80%);
}

.food-hero:hover::before,
.search-board:hover::before,
.food-card:hover::before {
  animation: feedSheen 960ms var(--motion-ease);
}

.food-hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
}

.hero-copy {
  max-width: 720px;
}

.food-hero h2 {
  margin-top: 8px;
  color: #f8fafc;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.08;
  font-weight: 900;
  letter-spacing: 0;
}

.module-subtitle {
  margin-top: 12px;
  color: #a7b0bf;
  font-size: 15px;
  line-height: 1.75;
}

.food-count {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-radius: 999px;
  color: #ff8ba0;
  background: rgba(255, 56, 92, 0.12);
  border: 1px solid rgba(255, 56, 92, 0.28);
}

.food-count strong {
  font-size: 14px;
  font-weight: 900;
}

.search-board {
  padding: 18px 20px;
}

.full-width {
  width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.food-grid {
  column-count: 3;
  column-gap: 16px;
  padding-top: 8px;
}

.food-card {
  width: 100%;
  display: grid;
  gap: 0;
  margin: 0 0 16px;
  overflow: hidden;
  break-inside: avoid;
  transform: translateZ(0);
  transition: transform 220ms var(--motion-spring), box-shadow 220ms ease, border-color 220ms ease;
}

.food-card:nth-child(3n + 2) {
  margin-top: 32px;
}

.food-card:nth-child(3n) {
  margin-top: 16px;
}

.food-card:hover {
  transform: translateY(-8px) scale(1.01);
  border-color: rgba(255, 56, 92, 0.36);
  box-shadow: 0 34px 90px rgba(0, 0, 0, 0.42), 0 0 0 1px rgba(255, 56, 92, 0.12);
}

.food-media {
  position: relative;
  aspect-ratio: var(--card-ratio, 4 / 5);
  overflow: hidden;
  background: #24272d;
}

.food-media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 520ms ease, filter 520ms ease;
}

.food-card:hover .food-media img {
  transform: scale(1.04);
  filter: saturate(1.08) contrast(1.02);
}

.food-media::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, transparent 48%, rgba(255, 56, 92, 0.14) 50%, transparent 52%),
    linear-gradient(180deg, transparent, rgba(13, 15, 18, 0.72));
  opacity: 0;
  transform: translateY(-18%);
}

.food-card:hover .food-media::after {
  animation: scanLine 920ms var(--motion-ease);
}

.food-media__overlay {
  position: absolute;
  inset: auto 12px 12px 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.food-rank,
.food-hot,
.food-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.food-rank {
  color: #ffffff;
  background: rgba(13, 15, 18, 0.78);
  backdrop-filter: blur(10px);
}

.food-hot {
  color: #ff8ba0;
  background: rgba(13, 15, 18, 0.72);
  backdrop-filter: blur(10px);
}

.food-body {
  display: grid;
  gap: 10px;
  padding: 16px 16px 18px;
}

.food-topline,
.food-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
}

.food-cuisine {
  color: #ff8ba0;
  font-size: 12px;
  font-weight: 800;
}

.food-destination {
  color: #a7b0bf;
  font-size: 12px;
}

.food-body h3 {
  color: #f8fafc;
  font-size: 18px;
  line-height: 1.28;
  font-weight: 900;
  letter-spacing: 0;
}

.food-body p {
  color: #d7dce5;
  font-size: 14px;
  line-height: 1.7;
}

.food-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.food-chip {
  color: #f8fafc;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
}

.food-stats {
  color: #a7b0bf;
  font-size: 12px;
}

.food-empty {
  padding: 16px;
}

@media (max-width: 1120px) {
  .food-grid {
    column-count: 2;
  }
}

@media (max-width: 860px) {
  .food-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    width: 100%;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .food-grid {
    column-count: 1;
  }

  .food-card:nth-child(n) {
    margin-top: 0;
  }

  .food-hero,
  .search-board {
    padding: 18px;
  }

  .food-hero h2 {
    font-size: 28px;
  }
}

@keyframes feedSheen {
  0% {
    opacity: 0;
    transform: translateX(-80%);
  }
  24%,
  60% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(80%);
  }
}

@keyframes scanLine {
  0% {
    opacity: 0;
    transform: translateY(-22%);
  }
  30%,
  68% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(22%);
  }
}
</style>
