<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createItinerary, listItineraries, updateItinerary } from '../api/travel'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const keyword = ref('')
const selectedRow = ref(null)
const detailOpen = ref(false)
const editOpen = ref(false)
const editMode = ref('create')
const editId = ref(null)

const form = reactive({
  name: '',
  owner: '',
  collaborators: '',
  strategy: '',
  transportMode: '',
  notes: '',
})

const resetForm = () => {
  form.name = ''
  form.owner = ''
  form.collaborators = ''
  form.strategy = ''
  form.transportMode = ''
  form.notes = ''
  editId.value = null
}

const normalize = (value) => (value || '').toString().trim().toLowerCase()

const filteredRows = computed(() => {
  const q = normalize(keyword.value)
  if (!q) return rows.value
  return rows.value.filter((item) => {
    return [
      item.name,
      item.owner,
      item.collaborators,
      item.strategy,
      item.transportMode,
      item.notes,
    ].some((field) => normalize(field).includes(q))
  })
})

const stats = computed(() => ({
  total: rows.value.length,
  collaborators: rows.value.filter((item) => (item.collaborators || '').trim()).length,
  strategies: new Set(rows.value.map((item) => item.strategy).filter(Boolean)).size,
  latest: rows.value[0]?.updatedAt || '-',
}))

const loadRows = async () => {
  loading.value = true
  try {
    const { data } = await listItineraries()
    rows.value = Array.isArray(data) ? data : []
    rows.value.sort((a, b) => String(b.updatedAt || '').localeCompare(String(a.updatedAt || '')))
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editMode.value = 'create'
  resetForm()
  editOpen.value = true
}

const openEdit = (row) => {
  editMode.value = 'edit'
  editId.value = row.id
  form.name = row.name || ''
  form.owner = row.owner || ''
  form.collaborators = row.collaborators || ''
  form.strategy = row.strategy || ''
  form.transportMode = row.transportMode || ''
  form.notes = row.notes || ''
  editOpen.value = true
}

const openDetail = (row) => {
  selectedRow.value = row
  detailOpen.value = true
}

const submit = async () => {
  if (!form.name.trim()) {
    ElMessage.warning('请输入行程名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      owner: form.owner.trim(),
      collaborators: form.collaborators.trim(),
      strategy: form.strategy.trim(),
      transportMode: form.transportMode.trim(),
      notes: form.notes.trim(),
    }
    if (editMode.value === 'edit') {
      await updateItinerary(editId.value, payload)
      ElMessage.success('行程已更新')
    } else {
      await createItinerary(payload)
      ElMessage.success('行程已创建')
    }
    editOpen.value = false
    await loadRows()
  } finally {
    saving.value = false
  }
}

const copySummary = async (row) => {
  const text = `名称: ${row.name || '-'}\n创建者: ${row.owner || '-'}\n协作者: ${row.collaborators || '-'}\n策略: ${row.strategy || '-'}\n交通: ${row.transportMode || '-'}\n备注: ${row.notes || '-'}`
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制摘要')
}

const confirmEdit = async (row) => {
  openEdit(row)
}

const refresh = async () => {
  await loadRows()
}

onMounted(loadRows)
</script>

<template>
  <section class="itinerary-page">
    <el-card class="module-card hero-card">
      <div class="hero-copy">
        <div class="eyebrow">协作行程</div>
        <h2>把路线、策略、备注和协作信息统一管理</h2>
        <p>支持快速检索、查看详情、复制摘要和在线编辑。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="openCreate">新建行程</el-button>
        <el-button size="large" @click="refresh">刷新</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-coral">
          <span>总行程</span>
          <strong>{{ stats.total }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-rose">
          <span>有协作者</span>
          <strong>{{ stats.collaborators }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-sand">
          <span>策略数</span>
          <strong>{{ stats.strategies }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-ink">
          <span>最近更新</span>
          <strong class="truncate">{{ stats.latest }}</strong>
        </div>
      </el-col>
    </el-row>

    <el-card class="module-card">
      <el-row :gutter="12" class="toolbar-row">
        <el-col :md="16" :xs="24">
          <el-input
            v-model="keyword"
            size="large"
            clearable
            placeholder="搜索名称、策略、备注、协作者"
          />
        </el-col>
        <el-col :md="8" :xs="24" class="toolbar-actions">
          <el-button @click="keyword = ''">清空</el-button>
          <el-button type="primary" @click="refresh">重新加载</el-button>
        </el-col>
      </el-row>

      <el-table :data="filteredRows" stripe border v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="owner" label="创建者" width="140" />
        <el-table-column prop="collaborators" label="协作者" min-width="160" />
        <el-table-column prop="strategy" label="策略" width="140" />
        <el-table-column prop="transportMode" label="交通" width="120" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="confirmEdit(row)">编辑</el-button>
            <el-button link @click="copySummary(row)">复制</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailOpen" title="行程详情" size="420px">
      <template v-if="selectedRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedRow.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建者">{{ selectedRow.owner || '-' }}</el-descriptions-item>
          <el-descriptions-item label="协作者">{{ selectedRow.collaborators || '-' }}</el-descriptions-item>
          <el-descriptions-item label="策略">{{ selectedRow.strategy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交通方式">{{ selectedRow.transportMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ selectedRow.updatedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ selectedRow.notes || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="editOpen" :title="editMode === 'create' ? '新建行程' : '编辑行程'" width="620px">
      <el-form label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="创建者">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="协作者">
          <el-input v-model="form.collaborators" placeholder="逗号分隔" />
        </el-form-item>
        <el-form-item label="策略">
          <el-input v-model="form.strategy" />
        </el-form-item>
        <el-form-item label="交通方式">
          <el-input v-model="form.transportMode" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.itinerary-page {
  display: grid;
  gap: 18px;
}

.hero-card {
  border-radius: 32px;
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: #ffffff;
  border: 1px solid #f0f0f0;
  box-shadow: rgba(0, 0, 0, 0.02) 0 0 0 1px, rgba(0, 0, 0, 0.05) 0 8px 20px;
}

.hero-copy h2 {
  margin-top: 8px;
  color: #222222;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.43;
  letter-spacing: 0;
}

.hero-copy p {
  margin-top: 8px;
  color: #6a6a6a;
  font-size: 14px;
}

.eyebrow {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff1f4;
  color: #ff385c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.hero-actions :deep(.el-button--primary) {
  border-color: #ff385c;
  background: #ff385c;
}

.hero-actions :deep(.el-button--primary:hover) {
  border-color: #ff5475;
  background: #ff5475;
}

.stats-row {
  margin-top: 2px;
}

.stat-card {
  padding: 16px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.stat-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #222222;
  font-size: 24px;
  font-weight: 700;
}

.stat-coral {
  background: linear-gradient(135deg, #fff8f8 0%, #fff1f4 100%);
  border-color: #ffd8e1;
}

.stat-rose {
  background: linear-gradient(135deg, #fff8fb 0%, #ffeef4 100%);
  border-color: #ffd9e6;
}

.stat-sand {
  background: linear-gradient(135deg, #fffaf1 0%, #fff4de 100%);
  border-color: #ffe5b3;
}

.stat-ink {
  background: linear-gradient(135deg, #f8f8fa 0%, #f1f1f4 100%);
  border-color: #e6e6ea;
}

.truncate {
  font-size: 16px !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toolbar-row {
  margin-bottom: 16px;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    justify-content: flex-start;
    margin-top: 8px;
  }
}
</style>
