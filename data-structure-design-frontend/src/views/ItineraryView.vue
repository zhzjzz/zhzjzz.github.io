<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Connection, Copy, Edit, Info, ListAdd, People, Plus, Refresh, Search, Timer } from '@icon-park/vue-next'
import {
  addItinerarySpotCandidate,
  createItinerary,
  getItinerary,
  listItineraries,
  listItineraryMapSpots,
  searchDestinations,
  submitItinerarySpotVote,
  updateItinerary,
} from '../api/travel'
import { useAppStore } from '../stores/app'
import { useItineraryCollaboration } from '../composables/useItineraryCollaboration'
import ConsensusProgress from '../components/itinerary/ConsensusProgress.vue'
import ItineraryPlannerPanel from '../components/itinerary/ItineraryPlannerPanel.vue'
import RealSpotSearchPanel from '../components/itinerary/RealSpotSearchPanel.vue'
import SpotDecisionCard from '../components/itinerary/SpotDecisionCard.vue'
import SquadPingFeed from '../components/itinerary/SquadPingFeed.vue'
import TacticalMapPanel from '../components/itinerary/TacticalMapPanel.vue'
import { buildRealSpotNodes, makePingText } from '../utils/itineraryVotes'
import itineraryDefaultImage from '../assets/defaults/itinerary-default.png'

const appStore = useAppStore()
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const keyword = ref('')
const selectedRow = ref(null)
const detailOpen = ref(false)
const editOpen = ref(false)
const editMode = ref('create')
const editId = ref(null)
const collabOpen = ref(false)
const collabLoading = ref(false)
const collabRow = ref(null)
const collabVersion = ref('')
const mapSpots = ref([])
const selectedNode = ref(null)
const plannerOpen = ref(false)
const voteSaving = ref(false)
const pingEvents = ref([])
const spotSearchKeyword = ref('')
const spotSearchResults = ref([])
const spotSearchLoading = ref(false)
const spotAddingId = ref(null)
const patchTimers = new Map()

const {
  connected,
  connecting,
  onlineUsers,
  activeEditors,
  lastError,
  events,
  connect,
  disconnect,
  sendEditing,
  sendPatch,
  sendSpotVote,
} = useItineraryCollaboration()

const form = reactive({
  name: '',
  owner: '',
  collaborators: '',
  strategy: '',
  transportMode: '',
  notes: '',
  updatedAt: null,
})

const collabForm = reactive({
  name: '',
  owner: '',
  collaborators: '',
  strategy: '',
  transportMode: '',
  notes: '',
})

const collabFields = [
  { key: 'name', label: '名称' },
  { key: 'owner', label: '创建者' },
  { key: 'collaborators', label: '协作者' },
  { key: 'strategy', label: '策略' },
  { key: 'transportMode', label: '交通方式' },
  { key: 'notes', label: '备注', type: 'textarea', rows: 5 },
]

const resetForm = () => {
  form.name = ''
  form.owner = ''
  form.collaborators = ''
  form.strategy = ''
  form.transportMode = ''
  form.notes = ''
  form.updatedAt = null
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

const currentEditorName = computed(() => appStore.user.name || collabForm.owner || '协作者')

const fallbackTacticalNodes = computed(() => {
  if (!collabRow.value?.id) return []
  const baseId = Number(collabRow.value.id) * 1000
  return [
    { spotId: baseId + 1, spotName: collabRow.value.name || '行程主节点', x: 18, y: 28 },
    { spotId: baseId + 2, spotName: collabRow.value.strategy || '路线策略', x: 45, y: 58 },
    { spotId: baseId + 3, spotName: collabRow.value.transportMode || '集合交通', x: 74, y: 34 },
  ].map((node) => ({
    ...node,
    votes: [],
    consensus: 'backup',
  }))
})

const tacticalNodes = computed(() => {
  return buildRealSpotNodes(mapSpots.value)
})

const selectedSpotId = computed(() => selectedNode.value?.spotId || null)

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

const applyItineraryToCollabForm = (itinerary) => {
  collabForm.name = itinerary.name || ''
  collabForm.owner = itinerary.owner || ''
  collabForm.collaborators = itinerary.collaborators || ''
  collabForm.strategy = itinerary.strategy || ''
  collabForm.transportMode = itinerary.transportMode || ''
  collabForm.notes = itinerary.notes || ''
  collabVersion.value = itinerary.updatedAt || ''
}

const updateRowInList = (itinerary) => {
  const index = rows.value.findIndex((row) => row.id === itinerary.id)
  if (index >= 0) {
    rows.value.splice(index, 1, itinerary)
  } else {
    rows.value.unshift(itinerary)
  }
  rows.value.sort((a, b) => String(b.updatedAt || '').localeCompare(String(a.updatedAt || '')))
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
  form.updatedAt = row.updatedAt || null
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
      payload.updatedAt = form.updatedAt
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

const syncSelectedNode = () => {
  const previousSpotId = selectedSpotId.value
  selectedNode.value = tacticalNodes.value.find((node) => String(node.spotId) === String(previousSpotId))
    || tacticalNodes.value[0]
    || null
}

const loadMapSpots = async (row) => {
  if (!row?.id) {
    mapSpots.value = []
    selectedNode.value = null
    return
  }
  const { data } = await listItineraryMapSpots(row.id)
  mapSpots.value = Array.isArray(data) ? data : []
  syncSelectedNode()
}

const searchRealSpots = async () => {
  const keyword = spotSearchKeyword.value.trim()
  if (!keyword) {
    spotSearchResults.value = []
    return
  }
  spotSearchLoading.value = true
  try {
    const { data } = await searchDestinations(keyword)
    spotSearchResults.value = Array.isArray(data)
      ? data
        .filter((item) => item.latitude != null
          && item.longitude != null
          && item.latitude !== ''
          && item.longitude !== ''
          && Number.isFinite(Number(item.latitude))
          && Number.isFinite(Number(item.longitude)))
        .slice(0, 10)
      : []
  } finally {
    spotSearchLoading.value = false
  }
}

const addRealSpot = async (spot) => {
  if (!collabRow.value?.id || !spot?.id) return
  spotAddingId.value = spot.id
  try {
    await addItinerarySpotCandidate(collabRow.value.id, { destinationId: spot.id })
    await loadMapSpots(collabRow.value)
    ElMessage.success('景点已加入协作地图')
  } finally {
    spotAddingId.value = null
  }
}

const selectNode = (node) => {
  selectedNode.value = node
}

const pushPing = (payload) => {
  const vote = payload?.vote || payload
  if (!vote?.spotId) return
  pingEvents.value.unshift({
    key: `${Date.now()}-${vote.spotId}-${vote.username || payload?.username || 'user'}`,
    username: vote.username || payload?.username || '队友',
    serverTimestamp: payload?.serverTimestamp || vote.updatedAt || vote.createdAt,
    text: makePingText(vote),
    vote,
  })
  pingEvents.value = pingEvents.value.slice(0, 8)
}

const applyVoteBroadcast = (payload) => {
  if (payload.type === 'SPOT_VOTE_REJECTED') {
    ElMessage.warning(payload.message || '投票未保存')
    return
  }
  if (payload.type !== 'SPOT_VOTE_UPDATED') return
  if (Array.isArray(payload.mapSpots)) {
    mapSpots.value = payload.mapSpots
  }
  pushPing(payload)
  syncSelectedNode()
}

const submitVote = async ({ spotId, spotName, voteType, reason }) => {
  if (!collabRow.value?.id) {
    ElMessage.warning('请先打开协作行程')
    return
  }
  voteSaving.value = true
  const payload = {
    spotId,
    spotName,
    username: currentEditorName.value,
    voteType,
    reason,
  }
  try {
    const sent = sendSpotVote(payload)
    if (!sent) {
      const { data } = await submitItinerarySpotVote(collabRow.value.id, payload)
      await loadMapSpots(collabRow.value)
      pushPing(data)
      ElMessage.success('投票已保存')
    }
  } finally {
    voteSaving.value = false
  }
}

const openCollaboration = async (row) => {
  collabOpen.value = true
  collabLoading.value = true
  plannerOpen.value = false
  try {
    const { data } = await getItinerary(row.id)
    collabRow.value = data
    applyItineraryToCollabForm(data)
    pingEvents.value = []
    spotSearchKeyword.value = ''
    spotSearchResults.value = []
    await loadMapSpots(data)
    connect({
      itineraryId: data.id,
      username: currentEditorName.value,
      onUpdated: (payload) => {
        if (payload.itinerary) {
          applyItineraryToCollabForm(payload.itinerary)
          updateRowInList(payload.itinerary)
          collabRow.value = payload.itinerary
          syncSelectedNode()
        }
      },
      onConflict: async (payload) => {
        ElMessage.warning(payload.message || '行程已被其他协作者更新')
        const latest = await getItinerary(row.id)
        applyItineraryToCollabForm(latest.data)
        updateRowInList(latest.data)
      },
      onSpotVoteUpdated: applyVoteBroadcast,
      onSpotVoteRejected: applyVoteBroadcast,
    })
  } finally {
    collabLoading.value = false
  }
}

const closeCollaboration = () => {
  patchTimers.forEach((timer) => clearTimeout(timer))
  patchTimers.clear()
  disconnect()
  collabOpen.value = false
  collabRow.value = null
  mapSpots.value = []
  selectedNode.value = null
  plannerOpen.value = false
  pingEvents.value = []
  spotSearchKeyword.value = ''
  spotSearchResults.value = []
}

const fieldEditor = (fieldKey) => {
  return activeEditors.value.find((item) => item.field === fieldKey)
}

const markEditing = (fieldKey) => {
  sendEditing(fieldKey)
}

const queuePatch = (fieldKey) => {
  if (!collabRow.value?.id) return
  clearTimeout(patchTimers.get(fieldKey))
  const timer = setTimeout(() => {
    const ok = sendPatch({
      field: fieldKey,
      value: collabForm[fieldKey],
      expectedUpdatedAt: collabVersion.value,
    })
    if (!ok) {
      ElMessage.warning('协作连接未建立，暂不能实时同步')
    }
  }, 700)
  patchTimers.set(fieldKey, timer)
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
        <h2>行程管理与多人同步编辑</h2>
        <p>把路线策略、交通方式、备注和协作者信息集中维护。</p>
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
            <button type="button" @click="openEdit(row)"><Edit theme="outline" size="14" fill="currentColor" /> 编辑</button>
            <button type="button" @click="openCollaboration(row)"><Connection theme="outline" size="14" fill="currentColor" /> 协作</button>
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

    <el-drawer
      v-model="collabOpen"
      title="协作编辑行程"
      size="86%"
      destroy-on-close
      @closed="closeCollaboration"
    >
      <div class="collab-panel" v-loading="collabLoading">
        <div class="collab-status">
          <el-tag :type="connected ? 'success' : connecting ? 'warning' : 'info'" effect="plain">
            {{ connected ? '已连接' : connecting ? '连接中' : '未连接' }}
          </el-tag>
          <span>当前身份：{{ currentEditorName }}</span>
          <span>版本：{{ formatDateTime(collabVersion) }}</span>
        </div>

        <div class="online-strip">
          <strong>在线协作者</strong>
          <el-tag v-for="user in onlineUsers" :key="user" size="small">{{ user }}</el-tag>
          <span v-if="!onlineUsers.length" class="muted">暂无在线成员</span>
        </div>

        <el-alert
          v-if="lastError"
          :title="lastError"
          type="warning"
          show-icon
          :closable="false"
        />

        <div class="tactical-layout">
          <div class="tactical-map-stack">
            <div class="planner-action-row">
              <span>{{ tacticalNodes.length }} 个真实景点</span>
              <el-button type="primary" :disabled="!tacticalNodes.length" @click="plannerOpen = !plannerOpen">
                一键规划
              </el-button>
            </div>
            <TacticalMapPanel
              :nodes="tacticalNodes"
              :selected-spot-id="selectedSpotId"
              @select-node="selectNode"
            />
          </div>
          <div class="tactical-side">
            <RealSpotSearchPanel
              v-model:keyword="spotSearchKeyword"
              :results="spotSearchResults"
              :loading="spotSearchLoading"
              :adding-id="spotAddingId"
              @search="searchRealSpots"
              @add="addRealSpot"
            />
            <ItineraryPlannerPanel v-if="plannerOpen" :spots="tacticalNodes" />
            <ConsensusProgress :nodes="tacticalNodes" />
            <SpotDecisionCard
              :node="selectedNode"
              :current-user="currentEditorName"
              :saving="voteSaving"
              @submit-vote="submitVote"
            />
            <SquadPingFeed :events="pingEvents" />
          </div>
        </div>

        <el-form label-width="96px" class="collab-form">
          <el-form-item v-for="field in collabFields" :key="field.key" :label="field.label">
            <div class="collab-input-wrap">
              <el-input
                v-model="collabForm[field.key]"
                :type="field.type || 'text'"
                :rows="field.rows"
                @focus="markEditing(field.key)"
                @input="queuePatch(field.key)"
              />
              <small v-if="fieldEditor(field.key)" class="editing-hint">
                {{ fieldEditor(field.key).username }} 正在编辑
              </small>
            </div>
          </el-form-item>
        </el-form>

        <div class="event-feed">
          <strong>同步记录</strong>
          <div v-if="!events.length" class="muted">暂无同步记录</div>
          <div v-for="event in events" :key="`${event.time}-${event.text}`" class="event-item">
            <span>{{ event.time }}</span>
            <p>{{ event.text }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
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
  grid-template-columns: minmax(180px, 1.25fr) minmax(76px, 0.44fr) minmax(98px, 0.62fr) minmax(88px, 0.56fr) minmax(70px, 0.42fr) 138px 284px;
  align-items: center;
  gap: 14px;
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
  gap: 7px;
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.itinerary-actions button {
  min-height: 32px;
  min-width: 54px;
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

.collab-panel {
  display: grid;
  gap: 16px;
}

.collab-status,
.online-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.collab-status {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
}

.online-strip {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.tactical-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: 14px;
  align-items: stretch;
}

.tactical-side {
  display: grid;
  gap: 14px;
}

.tactical-map-stack {
  display: grid;
  gap: 10px;
}

.planner-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.planner-action-row span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.collab-form {
  padding-top: 4px;
}

.collab-input-wrap {
  width: 100%;
  display: grid;
  gap: 6px;
}

.editing-hint {
  color: #ff5f7b;
  font-size: 12px;
}

.event-feed {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fafc;
}

.event-item {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #475569;
}

.event-item span {
  min-width: 46px;
  color: #94a3b8;
  font-size: 12px;
}

.event-item p {
  margin: 0;
}

.muted {
  color: #94a3b8;
  font-size: 13px;
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
    flex-wrap: wrap;
  }

  .tactical-layout {
    grid-template-columns: 1fr;
  }
}
</style>
