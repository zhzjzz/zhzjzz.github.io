<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MapDistance, Refresh, Search, Shop } from '@icon-park/vue-next'
import { listDestinations, listFacilities, searchNearbyFacilities } from '../api/travel'
import facilityDefaultImage from '../assets/defaults/facility-default.png'
import facilityFoodImage from '../assets/defaults/facility-food-default.png'
import facilityShopImage from '../assets/defaults/facility-shop-default.png'

const loading = ref(false)
const loadingTypeOptions = ref(false)
const destinations = ref([])
const results = ref([])
const facilityTypeOptions = ref([])

const form = ref({
  fromDestinationId: null,
  type: '',
  keyword: '',
  maxDistanceMeters: 1000,
})

const selectedDestinationName = computed(() => {
  const item = destinations.value.find((destination) => destination.id === form.value.fromDestinationId)
  return item?.name || '未选择'
})

const preferredStartNames = ['北京邮电大学沙河校区', '北京邮电大学']

const normalizeDestinationName = (name = '') => String(name).trim()

const findPreferredStartDestination = (items) => {
  const normalizedItems = items.map((item) => ({
    item,
    name: normalizeDestinationName(item.name),
  }))

  return (
    normalizedItems.find(({ name }) => preferredStartNames.some((target) => name === target))?.item ||
    normalizedItems.find(({ name }) => preferredStartNames.some((target) => name.includes(target)))?.item ||
    normalizedItems.find(({ name }) => name.includes('北京邮电') && name.includes('沙河'))?.item ||
    normalizedItems.find(({ name }) => name.includes('北京邮电'))?.item
  )
}

const facilityImage = (type = '') => {
  if (/咖啡|餐|食堂|饭店|美食|饮/.test(type)) return facilityFoodImage
  if (/商店|超市|购物|便利/.test(type)) return facilityShopImage
  return facilityDefaultImage
}

const loadFacilityTypeOptions = async (keyword = '') => {
  loadingTypeOptions.value = true
  try {
    const type = keyword.trim()
    const { data } = await listFacilities(type)
    facilityTypeOptions.value = [...new Set(data.map((item) => item.facilityType).filter(Boolean))]
  } catch (error) {
    console.error(error)
    ElMessage.error('加载设施类别失败，请稍后重试')
  } finally {
    loadingTypeOptions.value = false
  }
}

const loadDestinations = async () => {
  const { data } = await listDestinations()
  destinations.value = Array.isArray(data) ? data : []
  if (!form.value.fromDestinationId && destinations.value.length) {
    const defaultDestination = findPreferredStartDestination(destinations.value) || destinations.value[0]
    form.value.fromDestinationId = defaultDestination.id
  }
}

const search = async () => {
  const selectedDestination = destinations.value.find((destination) => destination.id === form.value.fromDestinationId)
  if (!selectedDestination || selectedDestination.latitude == null || selectedDestination.longitude == null) {
    ElMessage.warning('请先选择有坐标的起点目的地')
    return
  }

  loading.value = true
  try {
    const params = {
      fromLat: selectedDestination.latitude,
      fromLon: selectedDestination.longitude,
    }
    if (form.value.type) params.type = form.value.type
    if (form.value.keyword.trim()) params.keyword = form.value.keyword.trim()
    if (form.value.maxDistanceMeters) params.maxDistanceMeters = form.value.maxDistanceMeters

    const { data } = await searchNearbyFacilities(params)
    results.value = data
    ElMessage.success(`已找到 ${data.length} 个附近场所`)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadDestinations(), loadFacilityTypeOptions()])
  if (form.value.fromDestinationId) {
    await search()
  }
})
</script>

<template>
  <section class="facility-page">
    <el-card class="module-card facility-card">
      <div class="module-header">
        <div>
          <p class="demo-eyebrow">Nearby Search</p>
          <h2>场所查询</h2>
          <p class="module-subtitle">从起点目的地出发，按空间距离查询周边服务设施，并支持类别过滤和关键词检索。</p>
        </div>
      </div>

      <el-form :model="form" label-width="120px">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="起点目的地">
              <el-select v-model="form.fromDestinationId" class="full-width" placeholder="请选择起点目的地">
                <el-option
                  v-for="destination in destinations"
                  :key="destination.id"
                  :label="`${destination.name}（ID: ${destination.id}）`"
                  :value="destination.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :md="8" :xs="24">
            <el-form-item label="设施类别">
              <el-select
                v-model="form.type"
                class="full-width"
                clearable
                filterable
                remote
                allow-create
                default-first-option
                reserve-keyword
                :loading="loadingTypeOptions"
                placeholder="输入类别关键词搜索，留空可看全部类型"
                :remote-method="loadFacilityTypeOptions"
              >
                <el-option
                  v-for="item in facilityTypeOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="关键词">
              <el-input v-model="form.keyword" placeholder="名称/类别/所属地点" clearable />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="最大距离（米）">
              <el-input-number v-model="form.maxDistanceMeters" :min="100" :step="100" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="toolbar">
          <el-tag type="info" effect="plain">当前起点：{{ selectedDestinationName }}</el-tag>
          <div class="toolbar-actions">
            <el-button @click="loadDestinations">
              <Refresh theme="outline" size="16" fill="currentColor" />
              刷新目的地
            </el-button>
            <el-button type="primary" :loading="loading" @click="search">
              <Search theme="outline" size="16" fill="currentColor" />
              查询附近场所
            </el-button>
          </div>
        </div>

        <div class="result-summary">
          <span>当前起点：{{ selectedDestinationName }}</span>
          <strong><Shop theme="outline" size="18" fill="currentColor" /> {{ results.length }} 个结果</strong>
        </div>
      </el-form>

      <el-divider />

      <el-empty v-if="!results.length && !loading" description="当前条件下暂无可达设施" />
      <div v-else class="facility-results" v-loading="loading">
        <div class="facility-row facility-row-head">
          <span></span>
          <span>场所名称</span>
          <span>类别</span>
          <span>所属目的地</span>
          <span>距离</span>
        </div>
        <article v-for="(row, index) in results" :key="`${row.facility?.id || index}-${row.distanceMeters}`" class="facility-row">
          <img
            class="facility-thumb"
            :src="facilityImage(row.facility?.facilityType)"
            :alt="`${row.facility?.facilityType || '设施'}默认图`"
            loading="lazy"
          />
          <div class="facility-name">
            <strong>{{ row.facility?.name }}</strong>
            <small>{{ row.facility?.destination?.name || '未知目的地' }}</small>
          </div>
          <span class="facility-chip">{{ row.facility?.facilityType || '未分类' }}</span>
          <span class="facility-destination">{{ row.facility?.destination?.name || '-' }}</span>
          <strong class="facility-distance">
            <MapDistance theme="outline" size="15" fill="currentColor" />
            {{ Math.round(row.distanceMeters) }} m
          </strong>
        </article>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
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

.result-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  color: #a7b0bf;
}

.result-summary strong {
  color: #f8fafc;
  font-size: 18px;
}

.facility-results {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.2);
}

.facility-row {
  display: grid;
  grid-template-columns: 74px minmax(220px, 1.6fr) minmax(110px, 0.55fr) minmax(180px, 1fr) 110px;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #d7dce5;
}

.facility-thumb {
  width: 74px;
  height: 54px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.facility-row:nth-child(odd):not(.facility-row-head) {
  background: rgba(255, 255, 255, 0.035);
}

.facility-row:hover:not(.facility-row-head) {
  background: rgba(255, 56, 92, 0.08);
}

.facility-row-head {
  border-top: 0;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  font-size: 13px;
  font-weight: 900;
}

.facility-name strong,
.facility-name small {
  display: block;
}

.facility-name strong {
  color: #f8fafc;
  line-height: 1.4;
}

.facility-name small,
.facility-destination {
  color: #a7b0bf;
}

.facility-chip {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(243, 208, 138, 0.12);
  color: #f3d08a;
  font-size: 12px;
  font-weight: 800;
}

.facility-distance {
  justify-self: end;
  color: #f8fafc;
  font-size: 16px;
}

@media (max-width: 760px) {
  .facility-row,
  .facility-row-head {
    grid-template-columns: 1fr;
  }

  .facility-row-head {
    display: none;
  }

  .facility-distance {
    justify-self: start;
  }
}
</style>
