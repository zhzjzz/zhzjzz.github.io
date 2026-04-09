<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTopDestinations, searchDestinations } from '../api/travel'

/**
 * 检索关键字：支持按名称/类别模糊查询。
 */
const keyword = ref('')

/**
 * 推荐结果列表（表格数据源）。
 */
const rows = ref([])
const loading = ref(false)

/**
 * 读取 Top10 推荐结果。
 */
const loadTop = async () => {
  loading.value = true
  try {
    const { data } = await getTopDestinations(10)
    rows.value = data
  } finally {
    loading.value = false
  }
}

/**
 * 执行目的地检索，关键词为空时自动回退到 Top10。
 */
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
          <h2>目的地推荐</h2>
          <p class="module-subtitle">支持景区/校园目的地 Top-K 动态非完全排序推荐与关键词检索。</p>
        </div>
      </div>

      <el-row :gutter="12" class="toolbar-row">
        <el-col :md="12" :xs="24">
          <el-input
            v-model="keyword"
            placeholder="输入关键字（名称/类别）"
            clearable
            size="large"
            @keyup.enter="doSearch"
          />
        </el-col>
        <el-col :md="12" :xs="24" class="btn-group">
          <el-button type="primary" size="large" @click="doSearch">检索</el-button>
          <el-button size="large" @click="loadTop">Top10</el-button>
        </el-col>
      </el-row>

      <el-table :data="rows" stripe border v-loading="loading">
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
.destination-card {
  background:
    radial-gradient(circle at 0% 0%, rgba(125, 211, 252, 0.24), transparent 22%),
    radial-gradient(circle at 100% 100%, rgba(34, 197, 94, 0.2), transparent 24%),
    rgba(255, 255, 255, 0.9);
}

.toolbar-row {
  margin-bottom: 16px;
}

.btn-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .btn-group {
    justify-content: flex-start;
    margin-top: 8px;
  }
}
</style>
