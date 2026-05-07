<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDestinations, listFacilities, searchNearbyFacilities } from '../api/travel'

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
  destinations.value = data
  if (!form.value.fromDestinationId && data.length) {
    form.value.fromDestinationId = data[0].id
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
          <h2>场所查询</h2>
          <p class="module-subtitle">从起点目的地出发，按空间距离查找附近服务设施，并支持类别过滤与关键字检索。</p>
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
                placeholder="输入类别关键字搜索，留空可看全部类型"
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
            <el-form-item label="关键字">
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
            <el-button @click="loadDestinations">刷新目的地</el-button>
            <el-button type="primary" :loading="loading" @click="search">查询附近场所</el-button>
          </div>
        </div>
      </el-form>

      <el-divider />

      <el-empty v-if="!results.length && !loading" description="当前条件下暂无可达设施" />
      <el-table v-else :data="results" border stripe v-loading="loading">
        <el-table-column prop="facility.name" label="场所名称" min-width="180" />
        <el-table-column prop="facility.facilityType" label="类别" width="130" />
        <el-table-column prop="facility.destination.name" label="所属目的地" min-width="160" />
        <el-table-column prop="distanceMeters" label="距离（米）" width="150">
          <template #default="{ row }">
            {{ Math.round(row.distanceMeters) }}
          </template>
        </el-table-column>
      </el-table>
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
</style>
