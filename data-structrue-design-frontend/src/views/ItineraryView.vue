<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createItinerary, listItineraries } from '../api/travel'

/**
 * 行程列表数据，用于展示多人协作成果。
 */
const list = ref([])
const loading = ref(false)

/**
 * 行程创建表单：包含创建人、协作人、策略和备注等核心字段。
 */
const form = ref({
  name: '',
  owner: '演示用户',
  collaborators: '好友A,好友B',
  strategy: 'time',
  transportMode: 'walk+shuttle',
  notes: '',
})

/**
 * 拉取所有行程记录。
 */
const load = async () => {
  loading.value = true
  try {
    const { data } = await listItineraries()
    list.value = data
  } finally {
    loading.value = false
  }
}

/**
 * 创建新行程后刷新列表，形成实时协作感知。
 */
const submit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入行程名称')
    return
  }
  await createItinerary(form.value)
  form.value = {
    name: '',
    owner: '演示用户',
    collaborators: '好友A,好友B',
    strategy: 'time',
    transportMode: 'walk+shuttle',
    notes: '',
  }
  ElMessage.success('行程创建成功')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="itinerary-page">
    <el-card class="module-card itinerary-card">
      <div class="module-header">
        <div>
          <h2>行程多人协作</h2>
          <p class="module-subtitle">支持行程创建、协作成员管理、策略配置与实时保存。</p>
        </div>
      </div>

      <el-form :model="form" label-width="120px">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="行程名称">
              <el-input v-model="form.name" placeholder="如：北邮校园半日游" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="创建人">
              <el-input v-model="form.owner" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="协作成员">
              <el-input v-model="form.collaborators" placeholder="逗号分隔，如：小王,小李" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="form.strategy" class="full-width">
                <el-option label="最短时间" value="time" />
                <el-option label="最短距离" value="distance" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交通模式">
          <el-input v-model="form.transportMode" placeholder="例如 walk+shuttle" />
        </el-form-item>
        <el-form-item label="行程备注">
          <el-input v-model="form.notes" type="textarea" :rows="3" placeholder="记录停留时长、集合地点等信息" />
        </el-form-item>
        <el-button type="primary" @click="submit">创建行程</el-button>
      </el-form>

      <el-divider />

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="owner" label="创建人" width="120" />
        <el-table-column prop="collaborators" label="协作人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="strategy" label="策略" width="120" />
        <el-table-column prop="transportMode" label="交通" width="160" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.itinerary-card {
  background:
    radial-gradient(circle at 0% 10%, rgba(250, 204, 21, 0.2), transparent 24%),
    radial-gradient(circle at 95% 90%, rgba(16, 185, 129, 0.2), transparent 30%),
    rgba(255, 255, 255, 0.92);
}

.full-width {
  width: 100%;
}
</style>
