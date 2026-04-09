<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { planRoute } from '../api/travel'

/**
 * 路线规划请求参数：
 * - fromNodeId/toNodeId 为地图节点；
 * - strategy 为距离/时间策略；
 * - transport 为交通工具。
 */
const form = ref({ fromNodeId: 1, toNodeId: 2, strategy: 'time', transport: 'walk' })
const loading = ref(false)
const result = ref(null)

/**
 * 发起路线规划请求并展示返回结果。
 */
const submit = async () => {
  loading.value = true
  result.value = null
  try {
    const { data } = await planRoute(form.value)
    result.value = data
    ElMessage.success('路线规划完成')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '规划失败，请检查节点参数')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="route-page">
    <el-card class="module-card route-card">
      <div class="module-header">
        <div>
          <h2>路线规划</h2>
          <p class="module-subtitle">支持最短距离、最短时间及交通工具通行约束策略。</p>
        </div>
      </div>

      <el-form :model="form" label-width="120px" class="route-form">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="起点节点ID">
              <el-input-number v-model="form.fromNodeId" :min="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="终点节点ID">
              <el-input-number v-model="form.toNodeId" :min="1" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="form.strategy" class="full-width">
                <el-option label="最短距离" value="distance" />
                <el-option label="最短时间" value="time" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="交通工具">
              <el-select v-model="form.transport" class="full-width">
                <el-option label="步行" value="walk" />
                <el-option label="自行车" value="bike" />
                <el-option label="电瓶车" value="shuttle" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" :loading="loading" @click="submit">生成路线</el-button>
      </el-form>

      <el-divider />

      <el-empty v-if="!result" description="尚未生成路线，请先填写参数并点击按钮" />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="路径节点">
          {{ result.pathNodeIds?.join(' -> ') || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="总距离（米）">
          {{ result.totalDistanceMeters }}
        </el-descriptions-item>
        <el-descriptions-item label="预计耗时（分钟）">
          {{ result.totalTravelMinutes }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </section>
</template>

<style scoped>
.route-card {
  background:
    radial-gradient(circle at 95% 5%, rgba(14, 165, 233, 0.2), transparent 22%),
    radial-gradient(circle at 10% 90%, rgba(168, 85, 247, 0.2), transparent 30%),
    rgba(255, 255, 255, 0.9);
}

.full-width {
  width: 100%;
}

.route-form {
  max-width: 900px;
}
</style>
