<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Copy, DocAdd, Refresh, Search } from '@icon-park/vue-next'
import {
  addItinerarySpotCandidate,
  createItinerary,
  deleteItinerary,
  getItinerary,
  listItineraries,
  listItineraryMapSpots,
  searchDestinations,
  submitItinerarySpotVote,
} from '../api/travel'
import { useRoute } from 'vue-router'
import { useAppStore } from '../stores/app'
import { useItineraryCollaboration } from '../composables/useItineraryCollaboration'
import ConsensusProgress from '../components/itinerary/ConsensusProgress.vue'
import GuideImportDialog from '../components/itinerary/GuideImportDialog.vue'
import ItineraryPlannerPanel from '../components/itinerary/ItineraryPlannerPanel.vue'
import RealSpotSearchPanel from '../components/itinerary/RealSpotSearchPanel.vue'
import SpotDecisionCard from '../components/itinerary/SpotDecisionCard.vue'
import SquadPingFeed from '../components/itinerary/SquadPingFeed.vue'
import TacticalMapPanel from '../components/itinerary/TacticalMapPanel.vue'
import { buildRealSpotNodes, makePingText } from '../utils/itineraryVotes'
import itineraryDefaultImage from '../assets/defaults/itinerary-default.png'

const appStore = useAppStore()
const route = useRoute()
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const importOpen = ref(false)
const collabOpen = ref(false)
const collabLoading = ref(false)
const collabRow = ref(null)
const collabVersion = ref('')
const mapSpots = ref([])
const selectedNode = ref(null)
const plannerOpen = ref(false)
const plannerPreview = ref(null)
const voteSaving = ref(false)
const pingEvents = ref([])
const spotSearchKeyword = ref('')
const spotSearchResults = ref([])
const spotSearchLoading = ref(false)
const spotAddingId = ref(null)
const duplicatingId = ref(null)
const deletingId = ref(null)

const {
  connected,
  connecting,
  onlineUsers,
  lastError,
  events,
  connect,
  disconnect,
  sendSpotVote,
} = useItineraryCollaboration()

const collabForm = reactive({
  name: '',
  owner: '',
  collaborators: '',
  strategy: '',
  transportMode: '',
  notes: '',
})

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

const duplicateItinerary = async (row) => {
  if (!row?.id) return
  duplicatingId.value = row.id
  try {
    const { data: created } = await createItinerary({
      name: `${row.name || '行程'} 副本`,
      owner: row.owner || '',
      collaborators: row.collaborators || '',
      strategy: row.strategy || '',
      transportMode: row.transportMode || '',
      notes: row.notes || '',
    })
    const { data: spots } = await listItineraryMapSpots(row.id)
    await Promise.all((Array.isArray(spots) ? spots : [])
      .filter((spot) => spot.destinationId)
      .map((spot) => addItinerarySpotCandidate(created.id, { destinationId: spot.destinationId })))
    ElMessage.success('已新增相同行程')
    await loadRows()
  } finally {
    duplicatingId.value = null
  }
}

const removeItinerary = async (row) => {
  if (!row?.id) return
  const confirmed = await ElMessageBox.confirm(
    `Delete "${row.name || 'Untitled itinerary'}"? This itinerary will be removed from the list.`,
    'Delete itinerary',
    {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      distinguishCancelAndClose: true,
    }
  ).catch(() => false)
  if (!confirmed) return

  deletingId.value = row.id
  try {
    await deleteItinerary(row.id)
    rows.value = rows.value.filter((item) => item.id !== row.id)
    if (collabRow.value?.id === row.id) {
      closeCollaboration()
    }
    ElMessage.success('Itinerary deleted')
  } catch (error) {
    const status = error?.response?.status
    if (status === 404) {
      ElMessage.error('Delete API is not available or itinerary no longer exists. Restart the backend and retry.')
    } else {
      ElMessage.error(error?.response?.data?.message || error?.message || 'Delete failed')
    }
  } finally {
    deletingId.value = null
  }
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
    plannerPreview.value = null
    ElMessage.success('景点已加入协作地图')
  } finally {
    spotAddingId.value = null
  }
}

const selectNode = (node) => {
  selectedNode.value = node
}

const applyPlannerPreview = (preview) => {
  plannerPreview.value = preview
}

const handleImportCreated = async (response) => {
  if (!response?.itinerary?.id) return
  updateRowInList(response.itinerary)
  await openCollaboration(response.itinerary)
  plannerOpen.value = true
  plannerPreview.value = null
  ElMessage.success('Imported itinerary created')
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
  plannerPreview.value = null
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
  disconnect()
  collabOpen.value = false
  collabRow.value = null
  mapSpots.value = []
  selectedNode.value = null
  plannerOpen.value = false
  plannerPreview.value = null
  pingEvents.value = []
  spotSearchKeyword.value = ''
  spotSearchResults.value = []
}

const refresh = async () => {
  await loadRows()
}

const loadInitialRows = async () => {
  await loadRows()
  const openItinerary = route.query.openItinerary
  const requestedId = Array.isArray(openItinerary) ? openItinerary[0] : openItinerary
  if (!requestedId) return
  const row = rows.value.find((item) => String(item.id) === String(requestedId))
  if (row) {
    await openCollaboration(row)
    plannerOpen.value = true
  }
}

onMounted(loadInitialRows)
</script>

<template>
  <section class="itinerary-page">
    <el-card class="module-card hero-card">
      <div class="hero-copy">
        <div class="eyebrow">协作行程</div>
        <h2>行程协作与地图联动</h2>
        <p>围绕协作标注、真实景点和一键规划统一操作。</p>
      </div>
      <div class="hero-actions">
        <el-button class="guide-import-hero-button" size="large" @click="importOpen = true">
          <DocAdd theme="outline" size="19" fill="currentColor" />
          导入攻略
        </el-button>
        <el-button size="large" @click="refresh">
          <Refresh theme="outline" size="17" fill="currentColor" />
          刷新
        </el-button>
      </div>
      <img class="itinerary-hero-image" :src="itineraryDefaultImage" alt="行程默认封面" />
    </el-card>

    <el-card class="module-card">
      <el-row :gutter="12" class="toolbar-row">
        <el-col :md="16" :xs="24">
          <el-input
            v-model="keyword"
            size="large"
            clearable
            placeholder="搜索名称、创建者、备注"
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
          <span>更新时间</span>
          <span>操作</span>
        </div>
        <article v-for="row in filteredRows" :key="row.id || row.name" class="itinerary-row">
          <div class="itinerary-name">
            <strong>{{ row.name || '-' }}</strong>
            <small>{{ row.notes || '暂无备注' }}</small>
          </div>
          <span>{{ row.owner || '-' }}</span>
          <strong class="itinerary-time">{{ formatDateTime(row.updatedAt) }}</strong>
          <div class="itinerary-actions">
            <button type="button" class="collab-entry-button" @click="openCollaboration(row)"><Connection theme="outline" size="15" fill="currentColor" /> 协作</button>
            <button type="button" :disabled="duplicatingId === row.id" @click="duplicateItinerary(row)"><Copy theme="outline" size="14" fill="currentColor" /> {{ duplicatingId === row.id ? '生成中' : '复制' }}</button>
            <button type="button" class="delete-itinerary-button" :disabled="deletingId === row.id" @click="removeItinerary(row)">
              {{ deletingId === row.id ? '删除中' : '删除' }}
            </button>
          </div>
        </article>
      </div>
    </el-card>

    <el-drawer
      v-model="collabOpen"
      title="协作编辑行程"
      size="86%"
      destroy-on-close
      @closed="closeCollaboration"
    >
      <div class="collab-panel" v-loading="collabLoading">
        <div class="collab-command">
          <div>
            <span class="command-kicker">Live itinerary board</span>
            <h3>{{ collabRow?.name || '协作行程' }}</h3>
            <p>{{ tacticalNodes.length }} 个真实景点 · {{ connected ? '实时在线' : connecting ? '连接中' : '离线模式' }}</p>
          </div>
          <div class="command-meta">
            <el-tag :type="connected ? 'success' : connecting ? 'warning' : 'info'" effect="dark">
              {{ connected ? '已连接' : connecting ? '连接中' : '未连接' }}
            </el-tag>
            <span>{{ currentEditorName }}</span>
            <small>{{ formatDateTime(collabVersion) }}</small>
          </div>
          <div class="online-strip">
            <strong>在线协作者</strong>
            <el-tag v-for="user in onlineUsers" :key="user" size="small" effect="dark">{{ user }}</el-tag>
            <span v-if="!onlineUsers.length" class="muted">暂无在线成员</span>
          </div>
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
              :route-segments="plannerPreview?.segments || []"
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
            <ItineraryPlannerPanel v-if="plannerOpen" :spots="tacticalNodes" @planned="applyPlannerPreview" />
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

    <GuideImportDialog
      v-model="importOpen"
      :owner="currentEditorName"
      @created="handleImportCreated"
    />
  </section>
</template>

<style scoped>
.itinerary-page {
  display: grid;
  gap: 18px;
}

.itinerary-page :deep(.el-drawer.rtl) {
  width: min(1180px, 88vw) !important;
  background:
    linear-gradient(135deg, rgba(13, 18, 27, 0.98), rgba(18, 29, 43, 0.96)),
    #111827;
  color: #e5edf7;
  border-left: 1px solid rgba(56, 189, 248, 0.18);
  box-shadow: -30px 0 90px rgba(0, 0, 0, 0.46);
}

.itinerary-page :deep(.el-drawer__header) {
  margin: 0;
  padding: 18px 22px 12px;
  color: #f8fafc;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.itinerary-page :deep(.el-drawer__title) {
  color: #f8fafc;
  font-size: 18px;
  font-weight: 900;
}

.itinerary-page :deep(.el-drawer__body) {
  padding: 16px 20px 22px;
  background:
    linear-gradient(120deg, rgba(255, 56, 92, 0.1), transparent 32%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.64), rgba(2, 6, 23, 0.88));
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

.hero-actions :deep(.guide-import-hero-button) {
  min-width: 210px;
  height: 54px;
  padding: 0 28px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #0891b2 100%);
  color: #ffffff;
  font-size: 16px;
  font-weight: 900;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.32);
}

.hero-actions :deep(.guide-import-hero-button:hover),
.hero-actions :deep(.guide-import-hero-button:focus-visible) {
  background: linear-gradient(135deg, #1d4ed8 0%, #0e7490 100%);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 22px 48px rgba(37, 99, 235, 0.4);
}

.hero-actions :deep(.el-button--primary:hover) {
  border-color: #ff5475;
  background: #ff5475;
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
  grid-template-columns: minmax(280px, 1fr) minmax(92px, 140px) minmax(150px, 190px) minmax(190px, 240px);
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
  width: 100%;
  gap: 7px;
  justify-self: end;
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

.itinerary-actions button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.itinerary-actions .collab-entry-button {
  position: relative;
  min-width: 78px;
  border-color: rgba(255, 56, 92, 0.82);
  background: linear-gradient(135deg, #ff385c 0%, #ff7a18 100%);
  color: #ffffff;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1), 0 12px 28px rgba(255, 56, 92, 0.28);
}

.itinerary-actions .collab-entry-button:hover,
.itinerary-actions .collab-entry-button:focus-visible {
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.18), 0 16px 36px rgba(255, 56, 92, 0.42);
}

.itinerary-actions .delete-itinerary-button {
  border-color: rgba(248, 113, 113, 0.46);
  background: rgba(127, 29, 29, 0.28);
  color: #fca5a5;
}

.itinerary-actions .delete-itinerary-button:hover,
.itinerary-actions .delete-itinerary-button:focus-visible {
  color: #ffffff;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  box-shadow: 0 12px 30px rgba(220, 38, 38, 0.26);
}

.collab-panel {
  display: grid;
  gap: 14px;
  animation: collab-rise 360ms ease-out both;
}

.collab-command {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(30, 41, 59, 0.86)),
    rgba(15, 23, 42, 0.9);
  box-shadow: 0 22px 48px rgba(0, 0, 0, 0.25);
}

.command-kicker {
  color: #ff6b86;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.collab-command h3 {
  margin: 4px 0 4px;
  color: #f8fafc;
  font-size: 24px;
  line-height: 1.2;
}

.collab-command p {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
}

.command-meta {
  display: grid;
  justify-items: end;
  gap: 5px;
  color: #f8fafc;
  font-weight: 800;
}

.command-meta small {
  color: #94a3b8;
  font-size: 12px;
}

.online-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  grid-column: 1 / -1;
}

.online-strip {
  padding-top: 2px;
  color: #cbd5e1;
}

.tactical-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 360px);
  gap: 12px;
  align-items: stretch;
}

.tactical-side {
  display: grid;
  gap: 14px;
}

.tactical-side :deep(.real-spot-card),
.tactical-side :deep(.planner-panel),
.tactical-side :deep(.consensus-progress),
.tactical-side :deep(.decision-card),
.tactical-side :deep(.ping-feed) {
  border-color: rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(145deg, rgba(248, 250, 252, 0.96), rgba(226, 232, 240, 0.9));
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.18);
  animation: collab-card-pop 420ms ease-out both;
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
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.planner-action-row span {
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 800;
}

.event-feed {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(15, 23, 42, 0.74);
}

.event-item {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #cbd5e1;
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

@keyframes collab-rise {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.99);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes collab-card-pop {
  from {
    opacity: 0;
    transform: translateX(12px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
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
