<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createItinerary, getItinerary, listItineraries, updateItinerary } from '../api/travel'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const sharedId = ref('')
const syncTimer = ref(null)

const createForm = ref({
  name: '',
  owner: '演示用户',
  collaborators: '好友A,好友B',
  strategy: 'time',
  transportMode: 'walk+shuttle',
  notes: '',
})

const editForm = ref(null)
const serverSnapshot = ref(null)

const toEditPayload = (itinerary) => ({
  id: itinerary.id,
  name: itinerary.name || '',
  owner: itinerary.owner || '',
  collaborators: itinerary.collaborators || '',
  strategy: itinerary.strategy || 'time',
  transportMode: itinerary.transportMode || '',
  notes: itinerary.notes || '',
  updatedAt: itinerary.updatedAt || null,
})

const selectedId = computed(() => editForm.value?.id ?? null)

const isEditingDirty = computed(() => {
  if (!editForm.value || !serverSnapshot.value) {
    return false
  }
  return JSON.stringify({
    name: editForm.value.name,
    owner: editForm.value.owner,
    collaborators: editForm.value.collaborators,
    strategy: editForm.value.strategy,
    transportMode: editForm.value.transportMode,
    notes: editForm.value.notes,
  }) !== JSON.stringify({
    name: serverSnapshot.value.name,
    owner: serverSnapshot.value.owner,
    collaborators: serverSnapshot.value.collaborators,
    strategy: serverSnapshot.value.strategy,
    transportMode: serverSnapshot.value.transportMode,
    notes: serverSnapshot.value.notes,
  })
})

const applyServerItinerary = (itinerary) => {
  const payload = toEditPayload(itinerary)
  editForm.value = payload
  serverSnapshot.value = { ...payload }
}

const load = async () => {
  loading.value = true
  try {
    const { data } = await listItineraries()
    list.value = data

    if (!selectedId.value) {
      return
    }

    const latest = data.find((item) => item.id === selectedId.value)
    if (!latest) {
      editForm.value = null
      serverSnapshot.value = null
      return
    }

    if (serverSnapshot.value?.updatedAt !== latest.updatedAt) {
      if (isEditingDirty.value) {
        ElMessage.warning('检测到其他客户端已更新该行程，请先保存或点击“刷新为最新版本”')
      } else {
        applyServerItinerary(latest)
      }
    }
  } finally {
    loading.value = false
  }
}

const openById = async () => {
  const id = Number(sharedId.value)
  if (!Number.isInteger(id) || id <= 0) {
    ElMessage.warning('请输入正确的行程ID')
    return
  }
  const { data } = await getItinerary(id)
  applyServerItinerary(data)
  if (!list.value.some((item) => item.id === id)) {
    list.value = [data, ...list.value]
  }
  ElMessage.success('已打开共享行程')
}

const selectItinerary = (row) => {
  applyServerItinerary(row)
  sharedId.value = String(row.id)
}

const refreshCurrent = async () => {
  if (!selectedId.value) {
    return
  }
  const { data } = await getItinerary(selectedId.value)
  applyServerItinerary(data)
  ElMessage.success('已刷新为最新版本')
}

const submitCreate = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入行程名称')
    return
  }
  const { data } = await createItinerary(createForm.value)
  createForm.value = {
    name: '',
    owner: '演示用户',
    collaborators: '好友A,好友B',
    strategy: 'time',
    transportMode: 'walk+shuttle',
    notes: '',
  }
  ElMessage.success(`行程创建成功（ID: ${data.id}）`)
  await load()
}

const submitUpdate = async () => {
  if (!editForm.value) {
    ElMessage.warning('请先选择或打开行程')
    return
  }
  if (!editForm.value.name.trim()) {
    ElMessage.warning('行程名称不能为空')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: editForm.value.name,
      owner: editForm.value.owner,
      collaborators: editForm.value.collaborators,
      strategy: editForm.value.strategy,
      transportMode: editForm.value.transportMode,
      notes: editForm.value.notes,
      updatedAt: editForm.value.updatedAt,
    }
    const { data } = await updateItinerary(editForm.value.id, payload)
    applyServerItinerary(data)
    await load()
    ElMessage.success('行程已保存，其他客户端将自动同步')
  } catch (error) {
    if (error?.response?.status === 409) {
      ElMessage.error('保存失败：该行程已被其他客户端修改，正在刷新最新版本')
      await refreshCurrent()
      return
    }
    throw error
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
  syncTimer.value = setInterval(() => {
    if (selectedId.value) {
      load()
    }
  }, 3000)
})

onUnmounted(() => {
  if (syncTimer.value) {
    clearInterval(syncTimer.value)
  }
})
</script>

<template>
  <section class="itinerary-page">
    <el-card class="module-card itinerary-card">
      <div class="module-header">
        <div>
          <h2>行程多人协作</h2>
          <p class="module-subtitle">支持通过行程ID共享访问、多人同时编辑并自动同步。</p>
        </div>
      </div>

      <el-divider content-position="left">创建新行程</el-divider>
      <el-form :model="createForm" label-width="120px">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="行程名称">
              <el-input v-model="createForm.name" placeholder="如：北邮校园半日游" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="创建人">
              <el-input v-model="createForm.owner" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="协作成员">
              <el-input v-model="createForm.collaborators" placeholder="逗号分隔，如：小王,小李" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="createForm.strategy" class="full-width">
                <el-option label="最短时间" value="time" />
                <el-option label="最短距离" value="distance" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交通模式">
          <el-input v-model="createForm.transportMode" placeholder="例如 walk+shuttle" />
        </el-form-item>
        <el-form-item label="行程备注">
          <el-input v-model="createForm.notes" type="textarea" :rows="2" placeholder="记录停留时长、集合地点等信息" />
        </el-form-item>
        <el-button type="primary" @click="submitCreate">创建行程</el-button>
      </el-form>

      <el-divider content-position="left">打开/编辑共享行程</el-divider>
      <div class="toolbar">
        <el-input v-model="sharedId" placeholder="输入共享行程ID" class="id-input" clearable />
        <el-button @click="openById">打开行程</el-button>
        <el-button :disabled="!selectedId" @click="refreshCurrent">刷新为最新版本</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedId" @click="submitUpdate">保存当前修改</el-button>
      </div>

      <el-form v-if="editForm" :model="editForm" label-width="120px" class="edit-form">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="行程ID">
              <el-input :model-value="String(editForm.id)" disabled />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="最后更新时间">
              <el-input :model-value="editForm.updatedAt || '无'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="行程名称">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="创建人">
              <el-input v-model="editForm.owner" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="协作成员">
              <el-input v-model="editForm.collaborators" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="规划策略">
              <el-select v-model="editForm.strategy" class="full-width">
                <el-option label="最短时间" value="time" />
                <el-option label="最短距离" value="distance" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交通模式">
          <el-input v-model="editForm.transportMode" />
        </el-form-item>
        <el-form-item label="行程备注">
          <el-input v-model="editForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <el-divider />

      <el-table :data="list" border stripe v-loading="loading" @row-click="selectItinerary">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="owner" label="创建人" width="120" />
        <el-table-column prop="collaborators" label="协作人" min-width="180" show-overflow-tooltip />
        <el-table-column prop="strategy" label="策略" width="120" />
        <el-table-column prop="transportMode" label="交通" width="160" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
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
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.id-input {
  width: 220px;
}

.edit-form {
  margin-bottom: 12px;
}
</style>
