<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocAdd, Notebook, Search } from '@icon-park/vue-next'
import {
  createItineraryFromImport,
  listDiaries,
  previewItineraryImport,
  searchDiaryFullText,
} from '../../api/travel'
import {
  buildDiaryImportPayload,
  buildTextImportPayload,
  DEFAULT_GUIDE_IMPORT_DEMO_TEXT,
  importCanCreate,
  importSpotLabel,
} from '../../utils/guideImport'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  owner: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'created'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const sourceType = ref('TEXT')
const text = ref('')
const diaryKeyword = ref('')
const diaries = ref([])
const selectedDiaryId = ref(null)
const loadingDiaries = ref(false)
const previewing = ref(false)
const creating = ref(false)
const preview = ref(null)
const error = ref('')

const selectedDiary = computed(() => diaries.value.find((diary) => diary.id === selectedDiaryId.value) || null)
const canPreview = computed(() => {
  if (sourceType.value === 'TEXT') return text.value.trim().length > 0
  return Boolean(selectedDiaryId.value)
})
const canCreate = computed(() => importCanCreate(preview.value) && !creating.value)

watch(() => props.modelValue, (open) => {
  if (open && sourceType.value === 'DIARY' && !diaries.value.length) {
    void loadDiaries()
  }
})

watch(sourceType, (next) => {
  preview.value = null
  error.value = ''
  if (next === 'DIARY' && !diaries.value.length) {
    void loadDiaries()
  }
})

const close = () => {
  visible.value = false
}

const runDemo = async () => {
  sourceType.value = 'TEXT'
  text.value = DEFAULT_GUIDE_IMPORT_DEMO_TEXT
  preview.value = null
  await previewImport()
}

const loadDiaries = async () => {
  loadingDiaries.value = true
  try {
    const request = diaryKeyword.value.trim()
      ? searchDiaryFullText(diaryKeyword.value.trim())
      : listDiaries({ limit: 20 })
    const { data } = await request
    diaries.value = Array.isArray(data) ? data : []
  } catch (caught) {
    ElMessage.error(caught?.message || 'Failed to load diaries')
  } finally {
    loadingDiaries.value = false
  }
}

const importPayload = () => sourceType.value === 'TEXT'
  ? buildTextImportPayload(text.value, props.owner)
  : buildDiaryImportPayload(selectedDiaryId.value, props.owner)

const previewImport = async () => {
  if (!canPreview.value) {
    ElMessage.warning(sourceType.value === 'TEXT' ? 'Please paste guide text first' : 'Please choose a diary first')
    return
  }
  previewing.value = true
  error.value = ''
  try {
    const { data } = await previewItineraryImport(importPayload())
    preview.value = data
  } catch (caught) {
    error.value = caught?.response?.data?.message || caught?.message || 'Guide recognition failed'
    ElMessage.error(error.value)
  } finally {
    previewing.value = false
  }
}

const createImport = async () => {
  if (!canCreate.value) {
    ElMessage.warning('No matched spots are ready to create')
    return
  }
  creating.value = true
  try {
    const { data } = await createItineraryFromImport(importPayload())
    emit('created', data)
    close()
  } catch (caught) {
    error.value = caught?.response?.data?.message || caught?.message || 'Failed to create itinerary'
    ElMessage.error(error.value)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" width="780px" class="guide-import-dialog" align-center>
    <template #header>
      <span class="guide-import-scan" aria-hidden="true"></span>
      <div class="guide-import-header">
        <span class="guide-import-icon">
          <DocAdd theme="outline" size="22" fill="currentColor" />
        </span>
        <div>
          <strong>&#23548;&#20837;&#25915;&#30053;</strong>
          <small>&#25991;&#26412; / &#26085;&#35760;</small>
        </div>
      </div>
    </template>

    <el-tabs v-model="sourceType" class="guide-import-tabs">
      <el-tab-pane label="&#31896;&#36148;&#25991;&#26412;" name="TEXT">
        <el-input
          class="guide-import-textarea"
          v-model="text"
          type="textarea"
          :rows="8"
          maxlength="8000"
          show-word-limit
          placeholder="&#31896;&#36148;&#26053;&#28216;&#25915;&#30053;&#25991;&#23383;..."
        />
      </el-tab-pane>
      <el-tab-pane label="&#36873;&#25321;&#26085;&#35760;" name="DIARY">
        <div class="diary-picker">
          <el-input v-model="diaryKeyword" clearable placeholder="&#25628;&#32034;&#26085;&#35760;" @keyup.enter="loadDiaries">
            <template #prefix>
              <Search theme="outline" size="16" fill="currentColor" />
            </template>
          </el-input>
          <el-button :loading="loadingDiaries" @click="loadDiaries">&#25628;&#32034;</el-button>
        </div>
        <div class="diary-list" v-loading="loadingDiaries">
          <button
            v-for="diary in diaries"
            :key="diary.id"
            type="button"
            :class="['diary-choice', { active: diary.id === selectedDiaryId }]"
            @click="selectedDiaryId = diary.id"
          >
            <Notebook theme="outline" size="17" fill="currentColor" />
            <span>
              <strong>{{ diary.title || '\u672a\u547d\u540d\u65e5\u8bb0' }}</strong>
              <small>{{ diary.content || '\u6682\u65e0\u5185\u5bb9' }}</small>
            </span>
          </button>
          <el-empty v-if="!diaries.length" description="&#26242;&#26080;&#26085;&#35760;" />
        </div>
        <el-alert v-if="selectedDiary" type="info" :title="selectedDiary.title" :closable="false" show-icon />
      </el-tab-pane>
    </el-tabs>

    <el-alert v-if="error" type="warning" :title="error" :closable="false" show-icon />

    <section v-if="preview" class="import-preview">
      <header>
        <div>
          <strong>{{ preview.title || '\u5bfc\u5165\u884c\u7a0b' }}</strong>
          <span>{{ preview.summary || '\u6682\u65e0\u6458\u8981' }}</span>
        </div>
        <el-tag type="success">{{ preview.spots?.length || 0 }} &#20010;&#21305;&#37197;</el-tag>
      </header>

      <div class="matched-list">
        <article v-for="spot in preview.spots || []" :key="`${spot.matchedDestinationId}-${spot.orderIndex}`">
          <strong>{{ importSpotLabel(spot) }}</strong>
          <span>&#32622;&#20449;&#24230; {{ Math.round((spot.confidence || 0) * 100) }}%</span>
          <small>{{ spot.notes || spot.rawName }}</small>
        </article>
      </div>

      <div v-if="preview.unmatchedSpots?.length" class="unmatched-list">
        <el-tag v-for="spot in preview.unmatchedSpots" :key="`${spot.rawName}-${spot.orderIndex}`" type="warning" effect="plain">
          {{ spot.rawName }} / {{ spot.reason }}
        </el-tag>
      </div>

      <div v-if="preview.warnings?.length" class="unmatched-list">
        <el-tag v-for="warning in preview.warnings" :key="warning" type="warning" effect="plain">{{ warning }}</el-tag>
      </div>
    </section>

    <template #footer>
      <div class="guide-import-footer">
        <el-button @click="close">&#21462;&#28040;</el-button>
        <el-button :loading="previewing" @click="runDemo">&#19968;&#38190;&#28436;&#31034;</el-button>
        <el-button class="recognize-button" :loading="previewing" :disabled="!canPreview" @click="previewImport">
          <DocAdd theme="outline" size="16" fill="currentColor" />
          &#35782;&#21035;
        </el-button>
        <el-button class="generate-button" type="primary" :loading="creating" :disabled="!canCreate" @click="createImport">
          &#29983;&#25104;&#34892;&#31243;
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.guide-import-dialog.el-dialog) {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(17, 24, 39, 0.98), rgba(11, 18, 32, 0.98) 52%, rgba(17, 25, 29, 0.98));
  color: #f8fafc;
  box-shadow: 0 34px 90px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(255, 255, 255, 0.04) inset;
  animation: guideDialogIn 240ms ease-out both;
}

.guide-import-scan {
  position: absolute;
  top: 0;
  left: -35%;
  width: 32%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #38bdf8, #ff385c, transparent);
  content: '';
  animation: guideScan 2800ms ease-in-out infinite;
}

:global(.guide-import-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 22px 24px 14px;
}

:global(.guide-import-dialog .el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #d7dce5;
  transition: background 160ms ease, transform 160ms ease;
}

:global(.guide-import-dialog .el-dialog__headerbtn:hover) {
  background: rgba(255, 255, 255, 0.08);
  transform: rotate(90deg);
}

.guide-import-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.guide-import-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border: 1px solid rgba(56, 189, 248, 0.42);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.92), rgba(8, 145, 178, 0.9));
  color: #ffffff;
  box-shadow: 0 16px 38px rgba(37, 99, 235, 0.34);
}

.guide-import-header strong,
.guide-import-header small {
  display: block;
}

.guide-import-header strong {
  color: #f8fafc;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.2;
}

.guide-import-header small {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 700;
}

:global(.guide-import-dialog .el-dialog__body) {
  display: grid;
  gap: 16px;
  padding: 0 24px 18px;
}

:global(.guide-import-dialog .el-dialog__footer) {
  padding: 0 24px 24px;
}

.guide-import-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.guide-import-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: rgba(148, 163, 184, 0.18);
}

.guide-import-tabs :deep(.el-tabs__item) {
  height: 46px;
  color: #94a3b8;
  font-size: 15px;
  font-weight: 800;
}

.guide-import-tabs :deep(.el-tabs__item.is-active) {
  color: #f8fafc;
}

.guide-import-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #ff385c, #38bdf8);
  box-shadow: 0 0 18px rgba(56, 189, 248, 0.42);
}

.guide-import-textarea :deep(.el-textarea__inner) {
  min-height: 260px !important;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(11, 18, 32, 0.96)),
    repeating-linear-gradient(0deg, rgba(56, 189, 248, 0.04) 0, rgba(56, 189, 248, 0.04) 1px, transparent 1px, transparent 28px);
  color: #f8fafc;
  font-size: 15px;
  line-height: 1.7;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.22) inset;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.guide-import-textarea :deep(.el-textarea__inner:focus) {
  border-color: rgba(56, 189, 248, 0.72);
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.12), 0 18px 44px rgba(0, 0, 0, 0.22) inset;
}

.guide-import-textarea :deep(.el-input__count) {
  right: 14px;
  bottom: 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #94a3b8;
}

.diary-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.diary-picker :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.76);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.2) inset;
}

.diary-picker :deep(.el-input__inner) {
  color: #f8fafc;
}

.diary-picker :deep(.el-button) {
  min-width: 92px;
  height: 46px;
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.78);
  color: #f8fafc;
}

.diary-list,
.import-preview,
.matched-list,
.unmatched-list {
  display: grid;
  gap: 10px;
}

.diary-list {
  max-height: 300px;
  overflow: auto;
  padding-right: 2px;
}

.diary-choice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  width: 100%;
  padding: 13px 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.66);
  color: #d7dce5;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, box-shadow 160ms ease, transform 160ms ease;
}

.diary-choice:hover,
.diary-choice.active {
  border-color: rgba(56, 189, 248, 0.72);
  background: rgba(15, 23, 42, 0.94);
  transform: translateY(-1px);
  box-shadow: 0 16px 34px rgba(8, 145, 178, 0.16);
}

.diary-choice.active {
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.14), 0 16px 34px rgba(8, 145, 178, 0.16);
}

.diary-choice strong,
.diary-choice small,
.import-preview header strong,
.import-preview header span {
  display: block;
}

.diary-choice strong,
.import-preview header strong,
.matched-list strong {
  color: #f8fafc;
}

.diary-choice small {
  margin-top: 4px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.import-preview {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(56, 189, 248, 0.28);
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.88), rgba(8, 47, 73, 0.42));
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.24);
  animation: previewRise 220ms ease-out both;
}

.import-preview header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.matched-list article {
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.36);
  animation: previewRise 220ms ease-out both;
}

.matched-list strong,
.matched-list span,
.matched-list small {
  display: block;
}

.matched-list span,
.matched-list small,
.import-preview header span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.unmatched-list {
  display: flex;
  flex-wrap: wrap;
}

.guide-import-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.guide-import-footer :deep(.el-button) {
  min-width: 110px;
  height: 46px;
  margin-left: 0;
  border-radius: 13px;
  font-weight: 900;
}

.guide-import-footer :deep(.recognize-button) {
  min-width: 124px;
  border-color: rgba(56, 189, 248, 0.34);
  background: rgba(15, 23, 42, 0.92);
  color: #e0f2fe;
}

.guide-import-footer :deep(.recognize-button:hover) {
  border-color: rgba(56, 189, 248, 0.72);
  color: #ffffff;
  transform: translateY(-1px);
}

.guide-import-footer :deep(.generate-button) {
  min-width: 154px;
  border: 0;
  background: linear-gradient(135deg, #2563eb 0%, #0891b2 100%);
  color: #ffffff;
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.3);
}

.guide-import-footer :deep(.generate-button:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #0e7490 100%);
  transform: translateY(-1px);
  box-shadow: 0 20px 42px rgba(37, 99, 235, 0.38);
}

@keyframes guideDialogIn {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes guideScan {
  0%,
  22% {
    transform: translateX(0);
    opacity: 0;
  }

  35%,
  76% {
    opacity: 1;
  }

  100% {
    transform: translateX(420%);
    opacity: 0;
  }
}

@keyframes previewRise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  :global(.guide-import-dialog.el-dialog) {
    width: calc(100vw - 24px) !important;
    border-radius: 18px;
  }

  :global(.guide-import-dialog .el-dialog__body),
  :global(.guide-import-dialog .el-dialog__footer),
  :global(.guide-import-dialog .el-dialog__header) {
    padding-right: 16px;
    padding-left: 16px;
  }

  .diary-picker,
  .guide-import-footer {
    grid-template-columns: 1fr;
  }

  .guide-import-footer :deep(.el-button) {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  :global(.guide-import-dialog.el-dialog),
  .guide-import-scan,
  .import-preview,
  .matched-list article {
    animation: none;
  }

  :global(.guide-import-dialog .el-dialog__headerbtn),
  .diary-choice,
  .guide-import-footer :deep(.el-button) {
    transition: none;
  }
}
</style>
