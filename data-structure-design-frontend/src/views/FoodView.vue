<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bowl, Refresh, Search } from '@icon-park/vue-next'
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

const resultTitle = computed(() => `${foods.value.length} 个美食结果`)

const sortOptions = [
  { label: '综合推荐', value: 'recommend' },
  { label: '评分优先', value: 'rating' },
  { label: '目的地热度', value: 'destinationHeat' },
]

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
        <h2>美食推荐</h2>
        <p class="module-subtitle">按名称、菜系和目的地查找旅途中的顺路好味道。</p>
      </div>
      <div class="food-count">
        <Bowl theme="outline" size="20" fill="currentColor" />
        <strong>{{ resultTitle }}</strong>
      </div>
    </div>

    <el-card class="module-card food-filter-card">
      <el-form :model="form" label-width="88px" @submit.prevent>
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
          <el-tag type="info" effect="plain">推荐算法：评分 + 美食热度 + 目的地热度</el-tag>
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
    </el-card>

    <el-empty v-if="!foods.length && !loading" description="暂无匹配美食" />

    <div v-else class="food-grid" v-loading="loading">
      <article v-for="item in foods" :key="item.id || `${item.name}-${item.storeName}`" class="food-card">
        <div class="food-media">
          <img :src="item.imageUrl || foodDefaultImage" :alt="item.name || '美食图片'" loading="lazy" />
          <span>{{ item.cuisine || '地方风味' }}</span>
        </div>
        <div class="food-body">
          <div>
            <h3>{{ item.name }}</h3>
            <p>{{ item.storeName || '推荐店铺' }}</p>
          </div>
          <div class="food-meta">
            <strong>评分 {{ item.rating || '-' }}</strong>
            <span>热度 {{ Math.round(item.heat || 0) }}</span>
          </div>
          <div class="food-destination">{{ destinationName(item) }}</div>
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
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

.food-filter-card {
  border-radius: 8px;
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
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 220px;
}

.food-card {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

.food-media {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #24272d;
}

.food-media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.food-media span {
  position: absolute;
  left: 12px;
  bottom: 12px;
  max-width: calc(100% - 24px);
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(13, 15, 18, 0.78);
  color: #f3d08a;
  font-size: 12px;
  font-weight: 900;
}

.food-body {
  min-height: 178px;
  display: grid;
  gap: 14px;
  padding: 16px;
}

.food-body h3 {
  color: #f8fafc;
  font-size: 19px;
  line-height: 1.28;
  font-weight: 900;
}

.food-body p {
  margin-top: 6px;
  color: #a7b0bf;
  font-size: 14px;
}

.food-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #f8fafc;
}

.food-meta span {
  color: #ff385c;
  font-size: 13px;
  font-weight: 900;
}

.food-destination {
  align-self: end;
  color: #a7b0bf;
  font-size: 13px;
}

@media (max-width: 1128px) {
  .food-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

  .food-grid {
    grid-template-columns: 1fr;
  }
}
</style>
