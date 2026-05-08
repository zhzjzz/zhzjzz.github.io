<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Copy, Edit, Info, ListAdd, People, Plus, Refresh, Search, Timer } from '@icon-park/vue-next'
import { createItinerary, listItineraries, updateItinerary } from '../api/travel'
import itineraryDefaultImage from '../assets/defaults/itinerary-default.png'

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

const formatDateTime = (value) => {
  if (!value) return '-'
  const text = String(value)
  const match = text.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/)
  if (match) return `${match[1]} ${match[2]}`

  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

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
  latest: formatDateTime(rows.value[0]?.updatedAt),
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
        <el-button type="primary" size="large" @click="openCreate">
          <Plus theme="outline" size="17" fill="currentColor" />
          新建行程
        </el-button>
        <el-button size="large" @click="refresh">
          <Refresh theme="outline" size="17" fill="currentColor" />
          刷新
        </el-button>
      </div>
      <img class="itinerary-hero-image" :src="itineraryDefaultImage" alt="行程默认封面" />
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-coral">
          <span><ListAdd theme="outline" size="15" fill="currentColor" /> 总行程</span>
          <strong>{{ stats.total }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-rose">
          <span><People theme="outline" size="15" fill="currentColor" /> 有协作者</span>
          <strong>{{ stats.collaborators }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-sand">
          <span><Calendar theme="outline" size="15" fill="currentColor" /> 策略数</span>
          <strong>{{ stats.strategies }}</strong>
        </div>
      </el-col>
      <el-col :md="6" :xs="12">
        <div class="stat-card stat-ink">
          <span><Timer theme="outline" size="15" fill="currentColor" /> 最近更新</span>
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
          <el-button type="primary" @click="refresh">
            <Search theme="outline" size="16" fill="currentColor" />
            重新加载
          </el-button>
        </el-col>
      </el-row>

      <div class="itinerary-results" v-loading="loading">
        <div class="itinerary-row itinerary-row-head">
          <span>名称</span>
          <span>创建者</span>
          <span>协作者</span>
          <span>策略</span>
          <span>交通</span>
          <span>更新时间</span>
          <span>操作</span>
        </div>
        <article v-for="row in filteredRows" :key="row.id || row.name" class="itinerary-row">
          <div class="itinerary-name">
            <strong>{{ row.name || '-' }}</strong>
            <small>{{ row.notes || '暂无备注' }}</small>
          </div>
          <span>{{ row.owner || '-' }}</span>
          <span>{{ row.collaborators || '-' }}</span>
          <span class="itinerary-chip">{{ row.strategy || '未设置' }}</span>
          <span>{{ row.transportMode || '-' }}</span>
          <strong class="itinerary-time">{{ formatDateTime(row.updatedAt) }}</strong>
          <div class="itinerary-actions">
            <button type="button" @click="openDetail(row)"><Info theme="outline" size="14" fill="currentColor" /> 详情</button>
            <button type="button" @click="confirmEdit(row)"><Edit theme="outline" size="14" fill="currentColor" /> 编辑</button>
            <button type="button" @click="copySummary(row)"><Copy theme="outline" size="14" fill="currentColor" /> 复制</button>
          </div>
        </article>
      </div>
    </el-card>

    <el-drawer v-model="detailOpen" title="行程详情" size="420px">
      <template v-if="selectedRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedRow.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建者">{{ selectedRow.owner || '-' }}</el-descriptions-item>
          <el-descriptions-item label="协作者">{{ selectedRow.collaborators || '-' }}</el-descriptions-item>
          <el-descriptions-item label="策略">{{ selectedRow.strategy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交通方式">{{ selectedRow.transportMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(selectedRow.updatedAt) }}</el-descriptions-item>
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
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background:
    radial-gradient(circle at 82% 26%, rgba(255, 56, 92, 0.16), transparent 27%),
    radial-gradient(circle at 71% 74%, rgba(243, 208, 138, 0.12), transparent 24%),
    linear-gradient(135deg, #11151c 0%, #16283b 48%, #17191d 100%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.28);
}

.hero-card :deep(.el-card__body) {
  display: grid;
  grid-template-areas:
    'copy image'
    'actions image';
  grid-template-columns: minmax(0, 1fr) minmax(260px, 430px);
  align-items: center;
  column-gap: 34px;
  row-gap: 18px;
  width: 100%;
  padding: 34px 36px;
}

.hero-copy,
.hero-actions {
  position: relative;
  z-index: 1;
}

.hero-copy {
  grid-area: copy;
}

.itinerary-hero-image {
  grid-area: image;
  align-self: center;
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 20px;
  object-fit: cover;
  background: #11151c;
  box-shadow: 0 24px 58px rgba(0, 0, 0, 0.24);
}

.hero-copy h2 {
  margin-top: 8px;
  max-width: 760px;
  color: #f8fafc;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.24;
  letter-spacing: 0;
}

.hero-copy p {
  margin-top: 12px;
  max-width: 620px;
  color: #a7b0bf;
  font-size: 15px;
  line-height: 1.7;
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

.hero-actions {
  grid-area: actions;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.hero-actions :deep(.el-button) {
  margin-left: 0;
  min-width: 112px;
  height: 46px;
}

.hero-actions :deep(.el-button--primary:hover) {
  border-color: #ff5475;
  background: #ff5475;
}

.stats-row {
  margin-top: 2px;
  align-items: stretch;
}

.stats-row :deep(.el-col) {
  display: flex;
}

.stat-card {
  width: 100%;
  min-height: 116px;
  padding: 16px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.18);
  display: flex;
  flex-direction: column;
  justify-content: center;
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

.itinerary-results {
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.2);
}

.itinerary-row {
  display: grid;
  grid-template-columns: minmax(180px, 1.35fr) minmax(86px, 0.5fr) minmax(110px, 0.68fr) minmax(96px, 0.62fr) minmax(80px, 0.48fr) 150px 220px;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #d7dce5;
}

.itinerary-row:nth-child(odd):not(.itinerary-row-head) {
  background: rgba(255, 255, 255, 0.035);
}

.itinerary-row:hover:not(.itinerary-row-head) {
  background: rgba(255, 56, 92, 0.08);
}

.itinerary-row-head {
  border-top: 0;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  font-size: 13px;
  font-weight: 900;
}

.itinerary-name strong,
.itinerary-name small {
  display: block;
}

.itinerary-name strong {
  color: #f8fafc;
  line-height: 1.4;
}

.itinerary-name small {
  margin-top: 4px;
  color: #a7b0bf;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.itinerary-chip {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(243, 208, 138, 0.12);
  color: #f3d08a;
  font-size: 12px;
  font-weight: 800;
}

.itinerary-time {
  color: #f8fafc;
  font-size: 14px;
  white-space: nowrap;
}

.itinerary-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.itinerary-actions button {
  min-height: 32px;
  min-width: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid rgba(255, 95, 123, 0.22);
  border-radius: 8px;
  background: rgba(255, 95, 123, 0.07);
  color: #ff5f7b;
  padding: 0 8px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
}

.itinerary-actions button:hover,
.itinerary-actions button:focus-visible {
  color: #ff8ba0;
}

@media (max-width: 900px) {
  .hero-card {
    background: #17191d;
  }

  .hero-card :deep(.el-card__body) {
    grid-template-areas:
      'copy'
      'actions'
      'image';
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .hero-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .toolbar-actions {
    justify-content: flex-start;
    margin-top: 8px;
  }

  .itinerary-row,
  .itinerary-row-head {
    grid-template-columns: 1fr;
  }

  .itinerary-row-head {
    display: none;
  }

  .itinerary-actions {
    justify-content: flex-start;
  }
}
</style>
