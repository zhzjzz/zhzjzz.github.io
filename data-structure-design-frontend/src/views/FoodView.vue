<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bowl, Fire, MapDistance, Refresh, Search, Shop, Star } from '@icon-park/vue-next'
import { listDestinations, listFoodCuisines, listFoodPlaceAnchors, searchAmapFoods, searchFoods } from '../api/travel'
import foodDefaultImage from '../assets/defaults/food-default.png'

const loading = ref(false)
const optionLoading = ref(false)
const foods = ref([])
const destinations = ref([])
const placeAnchors = ref([])
const cuisines = ref([])

const form = ref({
  place: '天安门',
  keyword: '',
  cuisine: '',
  destinationId: null,
  sort: 'recommend',
  radiusMeters: 3000,
  priceRange: 'all',
  dataSource: 'amap',
  limit: 10,
})

const quickPlaces = ['天安门', '前门', '南锣鼓巷', '北京邮电大学']
const radiusOptions = [
  { label: '1 公里', value: 1000 },
  { label: '3 公里', value: 3000 },
  { label: '5 公里', value: 5000 },
  { label: '10 公里', value: 10000 },
]
const dataSourceOptions = [
  { label: '本地缓存', value: 'local' },
  { label: '高德实时', value: 'amap' },
]
const sortOptions = [
  { label: '综合推荐', value: 'recommend' },
  { label: '热度优先', value: 'heat' },
  { label: '评分优先', value: 'rating' },
  { label: '距离优先', value: 'distance' },
  { label: '人均低到高', value: 'averagePrice' },
  { label: '目的地热度', value: 'destinationHeat' },
]
const priceRanges = [
  { label: '不限价格', value: 'all', min: null, max: null },
  { label: '50 以下', value: '0-50', min: 0, max: 50 },
  { label: '50-100', value: '50-100', min: 50, max: 100 },
  { label: '100-200', value: '100-200', min: 100, max: 200 },
  { label: '200 以上', value: '200+', min: 200, max: null },
]

const selectedPriceRange = computed(() => priceRanges.find((item) => item.value === form.value.priceRange) || priceRanges[0])
const activePlace = computed(() => form.value.place.trim() || '当前位置')
const activeDataSourceLabel = computed(() => dataSourceOptions.find((item) => item.value === form.value.dataSource)?.label || '本地缓存')
const resultTitle = computed(() => `${foods.value.length} 个餐厅结果`)
const placeOptions = computed(() => {
  const seen = new Set()
  const options = []
  const add = (name, source) => {
    if (!name || seen.has(name)) return
    seen.add(name)
    options.push({ name, source })
  }
  quickPlaces.forEach((name) => add(name, '常用'))
  placeAnchors.value.forEach((item) => add(item.name, '景点'))
  destinations.value.forEach((item) => add(item.name, '目的地'))
  return options
})

const loadOptions = async () => {
  optionLoading.value = true
  try {
    const [destinationResponse, cuisineResponse, anchorResponse] = await Promise.all([
      listDestinations(),
      listFoodCuisines(),
      listFoodPlaceAnchors(),
    ])
    destinations.value = Array.isArray(destinationResponse.data) ? destinationResponse.data : []
    cuisines.value = Array.isArray(cuisineResponse.data) ? cuisineResponse.data.filter(Boolean) : []
    placeAnchors.value = Array.isArray(anchorResponse.data) ? anchorResponse.data.filter((item) => item?.name) : []
  } catch (error) {
    console.error(error)
    ElMessage.warning('筛选项加载失败，仍可直接搜索')
  } finally {
    optionLoading.value = false
  }
}

const searchParams = () => ({
  place: form.value.place.trim() || undefined,
  keyword: form.value.keyword.trim() || undefined,
  cuisine: form.value.cuisine || undefined,
  destinationId: form.value.destinationId || undefined,
  sort: form.value.sort,
  radiusMeters: form.value.radiusMeters,
  minAveragePrice: selectedPriceRange.value.min ?? undefined,
  maxAveragePrice: selectedPriceRange.value.max ?? undefined,
  limit: form.value.sort === 'recommend' ? Math.min(Number(form.value.limit) || 10, 10) : form.value.limit,
})

const search = async () => {
  loading.value = true
  try {
    const params = searchParams()
    const { data } = form.value.dataSource === 'amap'
      ? await searchAmapFoods(params)
      : await searchFoods(params)
    foods.value = applyClientSort(Array.isArray(data) ? data : [])
    ElMessage.success(`已找到 ${foods.value.length} 个餐厅`)
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
    place: '天安门',
    keyword: '',
    cuisine: '',
    destinationId: null,
    sort: 'recommend',
    radiusMeters: 3000,
    priceRange: 'all',
    dataSource: 'amap',
    limit: 10,
  }
  await search()
}

const searchPlace = async (place) => {
  form.value.place = place
  form.value.sort = 'distance'
  await search()
}

const selectCuisine = async (cuisine) => {
  form.value.cuisine = form.value.cuisine === cuisine ? '' : cuisine
  await search()
}

const applyClientSort = (items) => {
  const rows = [...items]
  if (form.value.sort === 'heat') {
    return rows.sort((a, b) => Number(b.heat || 0) - Number(a.heat || 0))
  }
  if (form.value.sort === 'rating') {
    return rows.sort((a, b) => Number(b.rating || 0) - Number(a.rating || 0))
  }
  if (form.value.sort === 'averagePrice') {
    return rows.sort((a, b) => Number(a.averagePrice || Infinity) - Number(b.averagePrice || Infinity))
  }
  if (form.value.sort === 'distance') {
    return rows.sort((a, b) => Number(a.distanceMeters || Infinity) - Number(b.distanceMeters || Infinity))
  }
  return rows
}

const destinationName = (item) => item.destination?.name || activePlace.value || '附近目的地'
const itemPlaceLabel = (item) => item.address || destinationName(item)
const foodImage = (item) => item.imageUrl || foodDefaultImage
const formatDistance = (meters) => {
  const value = Number(meters)
  if (!Number.isFinite(value)) return '距离待补充'
  if (value < 1000) return `${Math.round(value)} m`
  return `${(value / 1000).toFixed(value < 10000 ? 1 : 0)} km`
}
const formatPrice = (price) => {
  const value = Number(price)
  return Number.isFinite(value) && value > 0 ? `人均 ¥${Math.round(value)}` : '人均待补充'
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
        <h2>附近美食</h2>
        <p class="module-subtitle">按景点、距离、人均价格、店铺类型、热度和评分筛选餐厅。</p>
      </div>
      <div class="food-count">
        <Bowl theme="outline" size="20" fill="currentColor" />
        <strong>{{ resultTitle }}</strong>
      </div>
    </section>

    <section class="search-board reveal-in">
      <el-form :model="form" label-width="86px" @submit.prevent>
        <el-row :gutter="12">
          <el-col :lg="6" :md="12" :xs="24">
            <el-form-item label="附近地点">
              <el-select v-model="form.place" class="full-width" clearable filterable allow-create :loading="optionLoading" @change="search">
                <el-option v-for="item in placeOptions" :key="item.name" :label="item.name" :value="item.name">
                  <span>{{ item.name }}</span>
                  <small class="place-source">{{ item.source }}</small>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="餐厅/菜品">
              <el-input v-model="form.keyword" clearable placeholder="餐厅名、菜名" @keyup.enter="search" />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12" :xs="24">
            <el-form-item label="店铺类型">
              <el-select v-model="form.cuisine" class="full-width" clearable filterable :loading="optionLoading">
                <el-option v-for="item in cuisines" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12" :xs="24">
            <el-form-item label="排序">
              <el-select v-model="form.sort" class="full-width" @change="search">
                <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="范围">
              <el-select v-model="form.radiusMeters" class="full-width">
                <el-option v-for="item in radiusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="目的地">
              <el-select v-model="form.destinationId" class="full-width" clearable filterable :loading="optionLoading">
                <el-option v-for="destination in destinations" :key="destination.id" :label="destination.name" :value="destination.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="数据源">
              <el-select v-model="form.dataSource" class="full-width">
                <el-option v-for="item in dataSourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="8" :xs="24">
            <el-form-item label="人均价格">
              <el-select v-model="form.priceRange" class="full-width">
                <el-option v-for="item in priceRanges" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="数量">
              <el-input-number v-model="form.limit" :min="10" :max="200" :step="10" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="24" :xs="24">
            <div class="toolbar-actions">
              <el-button @click="reset">
                <Refresh theme="outline" size="16" fill="currentColor" />
                重置
              </el-button>
              <el-button type="primary" :loading="loading" @click="search">
                <Search theme="outline" size="16" fill="currentColor" />
                搜索餐厅
              </el-button>
            </div>
          </el-col>
        </el-row>

        <div class="quick-row" aria-label="常用地点">
          <button v-for="place in quickPlaces" :key="place" type="button" class="quick-chip" :class="{ active: form.place === place }" @click="searchPlace(place)">
            <MapDistance theme="outline" size="14" fill="currentColor" />
            {{ place }}
          </button>
        </div>

        <div class="quick-row" aria-label="店铺类型快捷筛选">
          <button v-for="item in cuisines.slice(0, 10)" :key="item" type="button" class="quick-chip" :class="{ active: form.cuisine === item }" @click="selectCuisine(item)">
            <Shop theme="outline" size="14" fill="currentColor" />
            {{ item }}
          </button>
        </div>
      </el-form>
    </section>

    <div class="result-context">
      <span><MapDistance theme="outline" size="16" fill="currentColor" /> {{ activePlace }}附近 {{ formatDistance(form.radiusMeters) }}</span>
      <span><Shop theme="outline" size="16" fill="currentColor" /> {{ activeDataSourceLabel }}</span>
      <span><Star theme="outline" size="16" fill="currentColor" /> 评分、热度、距离、人均价</span>
      <span>{{ selectedPriceRange.label }}</span>
    </div>

    <div v-if="!foods.length && !loading" class="food-empty">
      <el-empty description="暂无匹配美食" />
    </div>

    <div v-else class="food-grid" v-loading="loading">
      <article v-for="item in foods" :key="item.id || item.name" class="food-card">
        <img :src="foodImage(item)" :alt="item.name || '餐厅图片'" loading="lazy" />
        <div class="food-card__body">
          <div class="food-card__top">
            <span class="food-type">{{ item.cuisine || '餐饮' }}</span>
            <span><Fire theme="outline" size="14" fill="currentColor" /> {{ Math.round(Number(item.heat || 0)) }}</span>
          </div>
          <h3>{{ item.name || item.storeName }}</h3>
          <p>{{ itemPlaceLabel(item) }}</p>
          <div class="food-meta">
            <span><Star theme="outline" size="14" fill="currentColor" /> {{ item.rating || '-' }}</span>
            <span><MapDistance theme="outline" size="14" fill="currentColor" /> {{ formatDistance(item.distanceMeters) }}</span>
            <span>{{ formatPrice(item.averagePrice) }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.food-page {
  display: grid;
  gap: 18px;
}

.food-hero,
.search-board {
  padding: 22px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.food-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.food-hero h2 {
  margin: 6px 0;
  color: #111827;
  font-size: 30px;
  font-weight: 900;
}

.food-count,
.result-context,
.food-meta,
.food-card__top,
.quick-chip,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.food-count {
  padding: 12px 14px;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
}

.full-width {
  width: 100%;
}

.toolbar-actions {
  justify-content: flex-end;
  height: 100%;
}

.quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.quick-chip {
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 999px;
  padding: 7px 11px;
  background: #f8fafc;
  color: #475569;
  cursor: pointer;
}

.quick-chip.active {
  color: #ffffff;
  background: #ff385c;
  border-color: #ff385c;
}

.place-source {
  float: right;
  color: #94a3b8;
}

.result-context {
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.result-context span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.food-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.food-card {
  overflow: hidden;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.06);
}

.food-card img {
  width: 100%;
  aspect-ratio: 16 / 10;
  object-fit: cover;
}

.food-card__body {
  display: grid;
  gap: 9px;
  padding: 14px;
}

.food-card__top,
.food-meta {
  justify-content: space-between;
  color: #64748b;
  font-size: 12px;
}

.food-type {
  padding: 5px 8px;
  border-radius: 999px;
  background: #fff1f4;
  color: #ff385c;
  font-weight: 800;
}

.food-card h3 {
  color: #111827;
  font-size: 17px;
  font-weight: 900;
}

.food-card p {
  color: #64748b;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .food-hero,
  .toolbar-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
