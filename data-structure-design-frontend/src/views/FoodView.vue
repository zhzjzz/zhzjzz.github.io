<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bowl,
  Bread,
  Cake,
  ChickenLeg,
  ChopsticksFork,
  CoffeeMachine,
  Cup,
  Fire,
  ForkSpoon,
  Hamburger,
  KnifeFork,
  MapDistance,
  Noodles,
  Refresh,
  Search,
  Shop,
  Star,
  Tea,
} from '@icon-park/vue-next'
import { listDestinations, listFoodCuisines, searchFoods } from '../api/travel'
import { foodIconLabel, foodIconName } from '../utils/foodIcon'

const loading = ref(false)
const optionLoading = ref(false)
const foods = ref([])
const destinations = ref([])
const cuisines = ref([])

const form = ref({
  place: '天安门',
  keyword: '',
  cuisine: '',
  destinationId: null,
  sort: 'distance',
  radiusMeters: 3000,
  priceRange: 'all',
  limit: 30,
})

const quickPlaces = ['天安门', '前门', '南锣鼓巷', '北京邮电大学']
const knownPlaces = {
  天安门: { latitude: 39.9087, longitude: 116.3975 },
  天安门广场: { latitude: 39.9042, longitude: 116.3975 },
  前门: { latitude: 39.8990, longitude: 116.3979 },
  大栅栏: { latitude: 39.8936, longitude: 116.3926 },
  南锣鼓巷: { latitude: 39.9421, longitude: 116.4039 },
  北京邮电大学: { latitude: 39.9652, longitude: 116.3511 },
}

const radiusOptions = [
  { label: '1 公里', value: 1000 },
  { label: '3 公里', value: 3000 },
  { label: '5 公里', value: 5000 },
  { label: '10 公里', value: 10000 },
]

const sortOptions = [
  { label: '距离优先', value: 'distance' },
  { label: '综合推荐', value: 'recommend' },
  { label: '评分优先', value: 'rating' },
  { label: '人均低到高', value: 'averagePrice' },
  { label: '目的地热度', value: 'destinationHeat' },
]

const priceRanges = [
  { label: '不限价格', value: 'all', min: null, max: null },
  { label: '¥50 以下', value: '0-50', min: 0, max: 50 },
  { label: '¥50-100', value: '50-100', min: 50, max: 100 },
  { label: '¥100-200', value: '100-200', min: 100, max: 200 },
  { label: '¥200 以上', value: '200+', min: 200, max: null },
]

const foodIconComponents = {
  Bread,
  Cake,
  ChickenLeg,
  ChopsticksFork,
  CoffeeMachine,
  Cup,
  ForkSpoon,
  Hamburger,
  KnifeFork,
  Noodles,
  Tea,
}

const visualPalettes = [
  ['#18212f', '#f97316'],
  ['#102820', '#22c55e'],
  ['#241a12', '#eab308'],
  ['#16213f', '#38bdf8'],
  ['#2a173d', '#d946ef'],
  ['#2b1618', '#ef4444'],
  ['#102329', '#14b8a6'],
  ['#232016', '#a3e635'],
]

const resultTitle = computed(() => `${foods.value.length} 个餐馆结果`)
const activePlace = computed(() => form.value.place.trim() || '当前位置')
const selectedPriceRange = computed(() => priceRanges.find((item) => item.value === form.value.priceRange) || priceRanges[0])

const loadOptions = async () => {
  optionLoading.value = true
  try {
    const [destinationResponse, cuisineResponse] = await Promise.all([
      listDestinations(),
      listFoodCuisines(),
    ])
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
      place: form.value.place.trim() || undefined,
      keyword: form.value.keyword.trim() || undefined,
      cuisine: form.value.cuisine || undefined,
      destinationId: form.value.destinationId || undefined,
      sort: form.value.sort,
      radiusMeters: form.value.radiusMeters,
      minAveragePrice: selectedPriceRange.value.min ?? undefined,
      maxAveragePrice: selectedPriceRange.value.max ?? undefined,
      limit: form.value.limit,
    }
    const { data } = await searchFoods(params)
    foods.value = applyNearbyClientFilters(Array.isArray(data) ? data : [])
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
    sort: 'distance',
    radiusMeters: 3000,
    priceRange: 'all',
    limit: 30,
  }
  await search()
}

const searchPlace = async (place) => {
  form.value.place = place
  form.value.destinationId = null
  form.value.sort = 'distance'
  await search()
}

const selectCuisine = async (cuisine) => {
  form.value.cuisine = form.value.cuisine === cuisine ? '' : cuisine
  await search()
}

const destinationName = (item) => item.destination?.name || '附近目的地'

const normalizePlace = (value) => value.trim().toLowerCase().replace(/\s+/g, '')

const resolvePlace = () => {
  const place = form.value.place.trim()
  if (!place) return null
  const exact = knownPlaces[place]
  if (exact) return exact
  const key = normalizePlace(place)
  const entry = Object.entries(knownPlaces).find(([name]) => {
    const nameKey = normalizePlace(name)
    return nameKey.includes(key) || key.includes(nameKey)
  })
  return entry?.[1] || null
}

const foodLocation = (item) => {
  if (Number.isFinite(Number(item.latitude)) && Number.isFinite(Number(item.longitude))) {
    return { latitude: Number(item.latitude), longitude: Number(item.longitude) }
  }
  if (Number.isFinite(Number(item.destination?.latitude)) && Number.isFinite(Number(item.destination?.longitude))) {
    return {
      latitude: Number(item.destination.latitude),
      longitude: Number(item.destination.longitude),
    }
  }
  return null
}

const haversineMeters = (from, to) => {
  const radius = 6371000
  const dLat = ((to.latitude - from.latitude) * Math.PI) / 180
  const dLng = ((to.longitude - from.longitude) * Math.PI) / 180
  const lat1 = (from.latitude * Math.PI) / 180
  const lat2 = (to.latitude * Math.PI) / 180
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return radius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const applyNearbyClientFilters = (items) => {
  const anchor = resolvePlace()
  if (!anchor) return items
  const radius = Number(form.value.radiusMeters) || 3000
  const priceRange = selectedPriceRange.value
  const withDistance = items
    .map((item) => {
      const location = foodLocation(item)
      return {
        ...item,
        distanceMeters: location ? haversineMeters(anchor, location) : item.distanceMeters,
      }
    })
    .filter((item) => Number.isFinite(Number(item.distanceMeters)) && Number(item.distanceMeters) <= radius)
    .filter((item) => {
      const price = Number(item.averagePrice)
      if (!Number.isFinite(price)) return priceRange.value === 'all'
      return (priceRange.min == null || price >= priceRange.min)
        && (priceRange.max == null || price <= priceRange.max)
    })

  if (form.value.sort === 'rating') {
    return withDistance.sort((a, b) => Number(b.rating || 0) - Number(a.rating || 0))
  }
  if (form.value.sort === 'averagePrice') {
    return withDistance.sort((a, b) => {
      const priceDiff = Number(a.averagePrice || Infinity) - Number(b.averagePrice || Infinity)
      return priceDiff || Number(b.rating || 0) - Number(a.rating || 0)
    })
  }
  if (form.value.sort === 'destinationHeat') {
    return withDistance.sort((a, b) => Number(b.destination?.heat || 0) - Number(a.destination?.heat || 0))
  }
  return withDistance.sort((a, b) => {
    const distanceDiff = Number(a.distanceMeters || Infinity) - Number(b.distanceMeters || Infinity)
    return distanceDiff || Number(b.rating || 0) - Number(a.rating || 0)
  })
}

const formatPrice = (price) => {
  const value = Number(price)
  if (!Number.isFinite(value) || value <= 0) return '人均待补充'
  return `人均 ¥${Math.round(value)}`
}

const formatDistance = (meters) => {
  const value = Number(meters)
  if (!Number.isFinite(value)) return '距离待补充'
  if (value < 1000) return `${Math.round(value)} m`
  return `${(value / 1000).toFixed(value < 10000 ? 1 : 0)} km`
}

const visualKey = (item) => `${item.cuisine || ''}${item.storeName || ''}${item.name || ''}`

const paletteFor = (item) => {
  const key = visualKey(item)
  const seed = Array.from(key).reduce((sum, char) => sum + char.charCodeAt(0), 0)
  const [bg, accent] = visualPalettes[seed % visualPalettes.length]
  return { '--food-bg': bg, '--food-accent': accent }
}

const foodIconComponent = (item) => foodIconComponents[foodIconName(item)] || ChopsticksFork

onMounted(async () => {
  await loadOptions()
  await search()
})
</script>

<template>
  <section class="food-page">
    <div class="food-header">
      <div>
        <p class="demo-eyebrow">Local Taste</p>
        <h2>附近美食</h2>
        <p class="module-subtitle">按地点、距离、评分和店铺类型找餐馆。</p>
      </div>
      <div class="food-count">
        <Bowl theme="outline" size="20" fill="currentColor" />
        <strong>{{ resultTitle }}</strong>
      </div>
    </div>

    <el-card class="module-card food-filter-card">
      <el-form :model="form" label-width="92px" @submit.prevent>
        <el-row :gutter="12">
          <el-col :lg="7" :md="12" :xs="24">
            <el-form-item label="附近地点">
              <el-input
                v-model="form.place"
                clearable
                placeholder="例如：天安门 / 前门 / 南锣鼓巷"
                @keyup.enter="search"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="餐馆/菜品">
              <el-input
                v-model="form.keyword"
                clearable
                placeholder="餐馆名、菜名"
                @keyup.enter="search"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="店铺类型">
              <el-select
                v-model="form.cuisine"
                class="full-width"
                clearable
                filterable
                :loading="optionLoading"
                placeholder="全部类型"
              >
                <el-option v-for="item in cuisines" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="范围">
              <el-select v-model="form.radiusMeters" class="full-width">
                <el-option v-for="item in radiusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        </el-row>

        <el-row :gutter="12">
          <el-col :lg="8" :md="12" :xs="24">
            <el-form-item label="目的地">
              <el-select
                v-model="form.destinationId"
                class="full-width"
                clearable
                filterable
                :loading="optionLoading"
                placeholder="可选：绑定景点"
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
            <el-form-item label="人均价格">
              <el-select v-model="form.priceRange" class="full-width">
                <el-option v-for="item in priceRanges" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :xs="24">
            <el-form-item label="数量">
              <el-input-number v-model="form.limit" :min="6" :max="60" :step="6" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="24" :xs="24">
            <div class="toolbar-actions">
              <el-button @click="reset">
                <Refresh theme="outline" size="16" fill="currentColor" />
                重置
              </el-button>
              <el-button type="primary" :loading="loading" @click="search">
                <Search theme="outline" size="16" fill="currentColor" />
                搜索餐馆
              </el-button>
            </div>
          </el-col>
        </el-row>

        <div class="quick-row" aria-label="常用地点">
          <button
            v-for="place in quickPlaces"
            :key="place"
            type="button"
            class="quick-chip"
            :class="{ active: form.place === place }"
            @click="searchPlace(place)"
          >
            <MapDistance theme="outline" size="14" fill="currentColor" />
            {{ place }}
          </button>
        </div>

        <div class="quick-row cuisine-row" aria-label="店铺类型快捷筛选">
          <button
            v-for="item in cuisines.slice(0, 10)"
            :key="item"
            type="button"
            class="quick-chip"
            :class="{ active: form.cuisine === item }"
            @click="selectCuisine(item)"
          >
            <Shop theme="outline" size="14" fill="currentColor" />
            {{ item }}
          </button>
        </div>
      </el-form>
    </el-card>

    <div class="result-context">
      <span>
        <MapDistance theme="outline" size="16" fill="currentColor" />
        {{ activePlace }}附近 {{ formatDistance(form.radiusMeters) }}
      </span>
      <span>
        <Star theme="outline" size="16" fill="currentColor" />
        显示评分与距离
      </span>
      <span>
        <Shop theme="outline" size="16" fill="currentColor" />
        {{ selectedPriceRange.label }}
      </span>
    </div>

    <el-empty v-if="!foods.length && !loading" description="暂无匹配餐馆" />

    <div v-else class="food-list" v-loading="loading">
      <article v-for="item in foods" :key="item.id || `${item.name}-${item.storeName}`" class="food-card">
        <div class="food-media">
          <div class="food-icon-visual" :style="paletteFor(item)">
            <component :is="foodIconComponent(item)" theme="filled" size="70" fill="currentColor" />
            <span>{{ foodIconLabel(item) }}</span>
          </div>
          <small>真实图标</small>
        </div>
        <div class="food-body">
          <div class="food-title-row">
            <div>
              <h3>{{ item.storeName || item.name }}</h3>
              <p>{{ item.name }}</p>
            </div>
            <strong>{{ item.rating || '-' }}</strong>
          </div>
          <div class="food-tags">
            <span>{{ item.cuisine || '综合餐饮' }}</span>
            <span>{{ destinationName(item) }}</span>
          </div>
          <div class="food-metrics">
            <span>
              <MapDistance theme="outline" size="15" fill="currentColor" />
              {{ formatDistance(item.distanceMeters) }}
            </span>
            <span>
              <Fire theme="outline" size="15" fill="currentColor" />
              热度 {{ Math.round(item.heat || 0) }}
            </span>
            <span>
              <Shop theme="outline" size="15" fill="currentColor" />
              {{ formatPrice(item.averagePrice) }}
            </span>
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
  color: #f8fafc;
}

.food-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}

.food-header h2 {
  margin-top: 4px;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 900;
}

.food-count {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

.food-filter-card {
  border-radius: 8px;
}

.full-width {
  width: 100%;
}

.toolbar-actions {
  min-height: 32px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 10px;
}

.quick-row,
.result-context,
.food-tags,
.food-metrics {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-row {
  margin-top: 12px;
}

.cuisine-row {
  padding-top: 2px;
}

.quick-chip {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: #d7dee9;
  font-weight: 800;
  cursor: pointer;
}

.quick-chip.active,
.quick-chip:hover {
  border-color: rgba(56, 189, 248, 0.7);
  background: rgba(56, 189, 248, 0.16);
  color: #f8fafc;
}

.result-context {
  min-height: 42px;
  padding: 0 4px;
  color: #cbd5e1;
}

.result-context span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
}

.food-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  min-height: 220px;
}

.food-card {
  display: grid;
  grid-template-columns: 184px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

.food-media {
  position: relative;
  min-height: 170px;
  background: #24272d;
}

.food-icon-visual {
  width: 100%;
  height: 100%;
  min-height: 170px;
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 10px;
  place-items: center;
  align-content: center;
  background:
    radial-gradient(circle at 70% 18%, rgba(255, 255, 255, 0.24), transparent 30%),
    linear-gradient(135deg, var(--food-bg), var(--food-accent));
  color: #ffffff;
}

.food-icon-visual :deep(.i-icon) {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  color: #ffffff;
}

.food-icon-visual span {
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.22);
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
}

.food-media small {
  position: absolute;
  left: 12px;
  bottom: 12px;
  padding: 5px 8px;
  border-radius: 8px;
  background: rgba(13, 15, 18, 0.78);
  color: #f8fafc;
  font-size: 12px;
  font-weight: 900;
}

.food-body {
  min-width: 0;
  display: grid;
  gap: 16px;
  padding: 16px;
}

.food-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.food-title-row h3 {
  color: #f8fafc;
  font-size: 19px;
  line-height: 1.28;
  font-weight: 900;
}

.food-title-row p {
  margin-top: 6px;
  color: #a7b0bf;
  font-size: 14px;
}

.food-title-row strong {
  min-width: 52px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(250, 204, 21, 0.14);
  color: #fde68a;
  font-weight: 900;
}

.food-tags span,
.food-metrics span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  padding: 0 9px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 800;
}

.food-metrics span:first-child {
  color: #bfdbfe;
}

@media (max-width: 1128px) {
  .food-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 744px) {
  .food-header {
    align-items: stretch;
  }

  .food-count,
  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions {
    flex-direction: column;
  }

  .toolbar-actions .el-button {
    width: 100%;
  }

  .food-card {
    grid-template-columns: 1fr;
  }
}
</style>
