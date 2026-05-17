<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Bowl, Fire, MapDistance, Refresh, Search, Shop, Star } from '@icon-park/vue-next'
import { listDestinations, listFoodCuisines, listFoodPlaceAnchors, searchAmapFoods, searchFoods } from '../api/travel'
import foodDefaultImage from '../assets/defaults/food-default.png'

const amapSecret = (import.meta.env.VITE_AMAP_SECRET || '').trim()
if (amapSecret) {
  window._AMapSecurityConfig = { securityJsCode: amapSecret }
}

const AMAP_VERSION = '2.0'
const AMAP_PLUGINS = ['AMap.PlaceSearch']

const loading = ref(false)
const optionLoading = ref(false)
const foods = ref([])
const destinations = ref([])
const placeAnchors = ref([])
const cuisines = ref([])
const amapApi = ref(null)

const form = ref({
  place: '天安门',
  keyword: '',
  cuisine: '',
  destinationId: null,
  sort: 'distance',
  radiusMeters: 3000,
  priceRange: 'all',
  dataSource: 'amap',
  limit: 100,
})

const quickPlaces = ['天安门', '前门', '南锣鼓巷', '北京邮电大学']
const fallbackKnownPlaces = {
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
  { label: '20 公里', value: 20000 },
]

const dataSourceOptions = [
  { label: '高德实时', value: 'amap' },
  { label: '本地缓存', value: 'local' },
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

const resultTitle = computed(() => `${foods.value.length} 条餐馆结果`)
const activePlace = computed(() => form.value.place.trim() || '当前位置')
const selectedPriceRange = computed(() => priceRanges.find((item) => item.value === form.value.priceRange) || priceRanges[0])
const activeDataSourceLabel = computed(() => dataSourceOptions.find((item) => item.value === form.value.dataSource)?.label || '高德实时')
const placeOptions = computed(() => {
  const seen = new Set()
  const options = []
  const addOption = (name, latitude, longitude, source) => {
    if (!name || seen.has(name)) return
    seen.add(name)
    options.push({ name, latitude, longitude, source })
  }
  quickPlaces.forEach((name) => addOption(name, fallbackKnownPlaces[name]?.latitude, fallbackKnownPlaces[name]?.longitude, '常用'))
  placeAnchors.value.forEach((item) => addOption(item.name, item.latitude, item.longitude, '景点'))
  destinations.value.forEach((item) => addOption(item.name, item.latitude, item.longitude, '目的地'))
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
    ElMessage.warning('筛选项加载失败，仍可直接搜索美食')
  } finally {
    optionLoading.value = false
  }
}

const search = async () => {
  loading.value = true
  try {
    const params = searchParams()
    const data = form.value.dataSource === 'amap'
      ? await searchAmapJsFoods(params)
      : (await searchFoods(params)).data
    foods.value = applyNearbyClientFilters(Array.isArray(data) ? data : [])
  } catch (error) {
    console.error(error)
    if (form.value.dataSource === 'amap') {
      await searchWithFallback()
    } else {
      foods.value = []
      ElMessage.error('美食数据加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const searchWithFallback = async () => {
  try {
    const fallbackParams = searchParams()
    const backendAmap = await searchAmapFoods(fallbackParams).catch(() => null)
    const { data } = backendAmap || await searchFoods(fallbackParams)
    foods.value = applyNearbyClientFilters(Array.isArray(data) ? data : [])
    ElMessage.warning(backendAmap ? '浏览器高德搜索暂不可用，已显示服务端高德结果' : '高德实时搜索暂不可用，已显示本地缓存餐馆')
  } catch (fallbackError) {
    console.error(fallbackError)
    foods.value = []
    ElMessage.error('美食数据加载失败，请稍后重试')
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
  limit: form.value.limit,
})

const reset = async () => {
  form.value = {
    place: '天安门',
    keyword: '',
    cuisine: '',
    destinationId: null,
    sort: 'distance',
    radiusMeters: 3000,
    priceRange: 'all',
    dataSource: 'amap',
    limit: 100,
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

const destinationName = (item) => item.destination?.name || activePlace.value || '附近目的地'
const itemPlaceLabel = (item) => item.address || destinationName(item)

const loadAmapApi = async () => {
  if (amapApi.value) return amapApi.value
  const amapKey = (import.meta.env.VITE_AMAP_KEY || '').trim()
  if (!amapKey) {
    throw new Error('未配置 VITE_AMAP_KEY')
  }
  amapApi.value = await AMapLoader.load({
    key: amapKey,
    version: AMAP_VERSION,
    plugins: AMAP_PLUGINS,
  })
  return amapApi.value
}

const normalizePlace = (value) => value.trim().toLowerCase().replace(/\s+/g, '')

const resolvePlace = () => {
  const place = form.value.place.trim()
  if (!place) return null
  const option = placeOptions.value.find((item) => item.name === place)
  if (option && Number.isFinite(Number(option.latitude)) && Number.isFinite(Number(option.longitude))) {
    return { latitude: Number(option.latitude), longitude: Number(option.longitude) }
  }
  const key = normalizePlace(place)
  const fuzzy = placeOptions.value.find((item) => {
    const nameKey = normalizePlace(item.name)
    return (nameKey.includes(key) || key.includes(nameKey))
      && Number.isFinite(Number(item.latitude))
      && Number.isFinite(Number(item.longitude))
  })
  if (fuzzy) {
    return { latitude: Number(fuzzy.latitude), longitude: Number(fuzzy.longitude) }
  }
  const fallback = fallbackKnownPlaces[place]
  return fallback || null
}

const searchAmapJsFoods = async (params) => {
  const anchor = resolvePlace()
  if (!anchor) {
    throw new Error('暂不支持该地点的高德实时搜索，请选择下拉中的景点或切到本地缓存')
  }
  const AMap = await loadAmapApi()
  const safeLimit = Math.max(25, Math.min(Number(params.limit) || 100, 200))
  const pageSize = 25
  const pages = Math.ceil(safeLimit / pageSize)
  const all = []
  for (let pageIndex = 1; pageIndex <= pages; pageIndex += 1) {
    const pois = await amapPlaceSearchPage(AMap, anchor, params, pageIndex, pageSize)
    all.push(...pois)
    if (pois.length < pageSize || all.length >= safeLimit) break
  }
  return all.slice(0, safeLimit)
}

const amapPlaceSearchPage = (AMap, anchor, params, pageIndex, pageSize) => new Promise((resolve, reject) => {
  const placeSearch = new AMap.PlaceSearch({
    type: '餐饮服务',
    city: '北京',
    pageSize,
    pageIndex,
    extensions: 'all',
  })
  const keyword = params.keyword || ''
  const radius = Number(params.radiusMeters) || 5000
  placeSearch.searchNearBy(keyword, [anchor.longitude, anchor.latitude], radius, (status, result) => {
    if (status !== 'complete') {
      reject(new Error(result?.info || '高德餐饮搜索失败'))
      return
    }
    const pois = result?.poiList?.pois || []
    resolve(pois.map((poi) => normalizeAmapPoi(poi)).filter(Boolean))
  })
})

const normalizeAmapPoi = (poi) => {
  const lng = Number(poi.location?.lng)
  const lat = Number(poi.location?.lat)
  if (!poi.id || !poi.name || !Number.isFinite(lng) || !Number.isFinite(lat)) {
    return null
  }
  return {
    id: `amap-${poi.id}`,
    name: poi.name,
    storeName: poi.name,
    cuisine: cuisineFromAmapType(poi.type),
    address: typeof poi.address === 'string' ? poi.address : '',
    latitude: lat,
    longitude: lng,
    sourceType: 'amap-js',
    sourceId: poi.id,
    imageUrl: firstAmapPhotoUrl(poi.photos),
    rating: Number(poi.biz_ext?.rating) || null,
    averagePrice: Number(poi.biz_ext?.cost) || null,
    heat: Number(poi.biz_ext?.rating) ? Math.round(Number(poi.biz_ext.rating) * 18) : 70,
    distanceMeters: Number(poi.distance) || null,
  }
}

const firstAmapPhotoUrl = (photos) => {
  if (!Array.isArray(photos)) return null
  const photo = photos.find((item) => typeof item?.url === 'string' && item.url.trim())
  if (!photo) return null
  return normalizeImageUrl(photo.url)
}

const normalizeImageUrl = (url) => {
  const value = String(url || '').trim()
  if (!value) return null
  return value.replace(/^http:\/\/store\.is\.autonavi\.com/i, 'https://store.is.autonavi.com')
}

const cuisineFromAmapType = (type) => {
  const detail = String(type || '').split(';').filter(Boolean).pop() || ''
  if (detail.includes('北京菜')) return '京菜'
  if (detail.includes('中餐')) return '中餐'
  if (detail.includes('快餐')) return '快餐'
  if (detail.includes('咖啡')) return '咖啡'
  if (detail.includes('火锅')) return '火锅'
  if (detail.includes('甜品') || detail.includes('冷饮')) return '甜品'
  if (detail.includes('清真')) return '清真菜'
  if (detail.includes('西餐') || detail.includes('外国')) return '西式简餐'
  return detail || '餐饮'
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
  if (!anchor) return prioritizeImageResults(items)
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
    return prioritizeImageResults(withDistance.sort((a, b) => Number(b.rating || 0) - Number(a.rating || 0)))
  }
  if (form.value.sort === 'averagePrice') {
    return prioritizeImageResults(withDistance.sort((a, b) => {
      const priceDiff = Number(a.averagePrice || Infinity) - Number(b.averagePrice || Infinity)
      return priceDiff || Number(b.rating || 0) - Number(a.rating || 0)
    }))
  }
  if (form.value.sort === 'destinationHeat') {
    return prioritizeImageResults(withDistance.sort((a, b) => Number(b.destination?.heat || 0) - Number(a.destination?.heat || 0)))
  }
  return prioritizeImageResults(withDistance.sort((a, b) => {
    const distanceDiff = Number(a.distanceMeters || Infinity) - Number(b.distanceMeters || Infinity)
    return distanceDiff || Number(b.rating || 0) - Number(a.rating || 0)
  }))
}

const hasRealImage = (item) => typeof item?.imageUrl === 'string' && item.imageUrl.trim()

const prioritizeImageResults = (items) => {
  const withImage = []
  const withoutImage = []
  items.forEach((item) => {
    if (hasRealImage(item)) {
      withImage.push(item)
    } else {
      withoutImage.push(item)
    }
  })
  return [...withImage, ...withoutImage]
}

const ratingText = (item) => {
  const rating = Number(item?.rating)
  if (!Number.isFinite(rating) || rating <= 0) return '-'
  return rating.toFixed(1)
}

const heatText = (item) => Math.round(item?.heat || 0)

const foodExcerpt = (item) => {
  const storeName = String(item?.storeName || '').trim()
  if (!storeName) return itemPlaceLabel(item)
  return `${storeName} · ${itemPlaceLabel(item)}`
}

const foodTags = (item) => {
  const tags = []
  if (item?.cuisine) tags.push(item.cuisine)
  if (item?.sourceType) tags.push(sourceLabel(item.sourceType))
  if (Number.isFinite(Number(item.averagePrice))) tags.push(formatPrice(item.averagePrice))
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

const sourceLabel = (sourceType) => {
  if (sourceType === 'amap-js' || sourceType === 'amap-live') return '高德实时'
  if (sourceType === 'osm') return '开放地图'
  return sourceType || '本地缓存'
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
        <p class="module-subtitle">搜索真实餐馆，按景点、距离、人均价格和店铺类型快速筛选。</p>
      </div>
      <div class="food-count">
        <Bowl theme="outline" size="20" fill="currentColor" />
        <strong>{{ resultTitle }}</strong>
      </div>
    </section>

    <section class="search-board reveal-in">
      <el-form :model="form" label-width="78px" @submit.prevent>
        <el-row :gutter="12">
          <el-col :lg="6" :md="12" :xs="24">
            <el-form-item label="附近地点">
              <el-select
                v-model="form.place"
                class="full-width"
                clearable
                filterable
                allow-create
                default-first-option
                :loading="optionLoading"
                placeholder="搜索或选择所有景点"
                @change="search"
              >
                <el-option
                  v-for="item in placeOptions"
                  :key="item.name"
                  :label="item.name"
                  :value="item.name"
                >
                  <span>{{ item.name }}</span>
                  <small class="place-source">{{ item.source }}</small>
                </el-option>
              </el-select>
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
          <el-col :lg="4" :md="12" :xs="24">
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
          <el-col :lg="4" :md="12" :xs="24">
            <el-form-item label="范围">
              <el-select v-model="form.radiusMeters" class="full-width">
                <el-option v-for="item in radiusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12" :xs="24">
            <el-form-item label="排序">
              <el-select v-model="form.sort" class="full-width">
                <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :lg="6" :md="12" :xs="24">
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
              <el-input-number v-model="form.limit" :min="25" :max="200" :step="25" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="24" :xs="24">
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

        <div class="quick-row" aria-label="店铺类型快捷筛选">
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
    </section>

    <div class="result-context">
      <span>
        <MapDistance theme="outline" size="16" fill="currentColor" />
        {{ activePlace }}附近 {{ formatDistance(form.radiusMeters) }}
      </span>
      <span>
        <Shop theme="outline" size="16" fill="currentColor" />
        {{ activeDataSourceLabel }}
      </span>
      <span>
        <Star theme="outline" size="16" fill="currentColor" />
        评分、距离、人均价
      </span>
      <span>{{ selectedPriceRange.label }}</span>
    </div>

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
            <span class="food-destination">{{ formatDistance(item.distanceMeters) }}</span>
          </div>
          <h3>{{ item.storeName || item.name }}</h3>
          <p>{{ foodExcerpt(item) }}</p>
          <div class="food-tags">
            <span v-for="tag in foodTags(item)" :key="tag" class="food-chip">{{ tag }}</span>
          </div>
          <div class="food-stats">
            <span>评分 {{ ratingText(item) }}</span>
            <span>{{ formatPrice(item.averagePrice) }}</span>
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
  display: grid;
  gap: 18px;
  color: #f8fafc;
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
  border: 1px solid rgba(255, 56, 92, 0.28);
  border-radius: 999px;
  color: #ff8ba0;
  background: rgba(255, 56, 92, 0.12);
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

.toolbar-actions {
  min-height: 32px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.quick-row,
.result-context {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-row {
  margin-top: 12px;
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
  border-color: rgba(255, 56, 92, 0.54);
  background: rgba(255, 56, 92, 0.14);
  color: #f8fafc;
}

.place-source {
  float: right;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
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
