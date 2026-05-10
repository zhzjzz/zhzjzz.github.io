<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Fire, Globe, Like, Lock, MagicWand, Message, Refresh, Search, Send, Share, Star, UploadPicture } from '@icon-park/vue-next'
import {
  createDiary,
  createDiaryComment,
  deleteDiary,
  getDiary,
  interactDiary,
  listDiaries,
  listDiaryComments,
  listHotDiaries,
  searchDiaryFullText,
} from '../api/travel'
import { fetchApiAssetBlobUrl, resolveApiAssetUrl } from '../api/http'
import diaryDefaultImage from '../assets/defaults/diary-default.png'
import aigcDefaultImage from '../assets/defaults/aigc-animation-default.png'
import { useAppStore } from '../stores/app'

const diaries = ref([])
const hotDiaries = ref([])
const comments = ref({})
const diaryDetailCache = ref(new Map())
const commentCache = ref(new Map())
const loading = ref(false)
const submitting = ref(false)
const searchKeyword = ref('')
const selectedDiaryId = ref(null)
const selectedMediaName = ref('')
const mediaPreview = ref('')
const selectedDiaryImageUrl = ref('')
const diaryImageUrlCache = new Map()
const detailRequests = new Map()
const commentForm = ref({ authorName: '游客', content: '' })
const DIARY_IMAGE_MAX_EDGE = 1600
const DIARY_IMAGE_QUALITY = 0.86
const appStore = useAppStore()
const form = ref({
  title: '',
  content: '',
  mediaType: 'image',
  mediaUrl: '',
  originalSizeBytes: null,
  score: 4.5,
  views: 0,
  isPublic: true,
})

const selectedDiary = computed(() => {
  if (!selectedDiaryId.value) return diaries.value[0] || null
  return diaryDetailCache.value.get(selectedDiaryId.value) || diaries.value.find((item) => item.id === selectedDiaryId.value) || diaries.value[0] || null
})
const canDeleteSelectedDiary = computed(() => Boolean(selectedDiary.value?.id && appStore.user.name))

const cacheDiary = (diary) => {
  if (!diary?.id) return null
  const cached = { ...(diaryDetailCache.value.get(diary.id) || {}), ...diary }
  diaryDetailCache.value.set(diary.id, cached)
  const diaryIndex = diaries.value.findIndex((item) => item.id === diary.id)
  if (diaryIndex >= 0) diaries.value.splice(diaryIndex, 1, { ...diaries.value[diaryIndex], ...cached })
  const hotIndex = hotDiaries.value.findIndex((item) => item.id === diary.id)
  if (hotIndex >= 0) hotDiaries.value.splice(hotIndex, 1, { ...hotDiaries.value[hotIndex], ...cached })
  return cached
}

const seedDiaryCache = (items) => {
  items.forEach((item) => {
    if (!item?.id) return
    diaryDetailCache.value.set(item.id, { ...(diaryDetailCache.value.get(item.id) || {}), ...item })
  })
}

const resetForm = () => {
  form.value = {
    title: '',
    content: '',
    mediaType: 'image',
    mediaUrl: '',
    originalSizeBytes: null,
    score: 4.5,
    views: 0,
    isPublic: true,
  }
  selectedMediaName.value = ''
  mediaPreview.value = ''
}

const load = async () => {
  loading.value = true
  try {
    const previousId = selectedDiaryId.value
    const [{ data: diaryData }, { data: hotData }] = await Promise.all([listDiaries(), listHotDiaries(6)])
    diaries.value = Array.isArray(diaryData) ? diaryData : []
    hotDiaries.value = Array.isArray(hotData) ? hotData : []
    seedDiaryCache(diaries.value)
    seedDiaryCache(hotDiaries.value)
    selectedDiaryId.value = diaries.value.some((item) => item.id === previousId) ? previousId : diaries.value[0]?.id || null
    loadDiaryDetail(selectedDiaryId.value)
  } catch (error) {
    console.error(error)
    ElMessage.error('日记数据加载失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }

  const payload = {
    ...form.value,
    title: form.value.title.trim(),
    content: form.value.content.trim(),
  }
  submitting.value = true
  let data
  try {
    ;({ data } = await createDiary(payload))
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.status === 401 ? '登录状态已失效，请重新登录后发布' : '发布日记失败，请稍后重试')
    return
  } finally {
    submitting.value = false
  }
  resetForm()
  const compressionSummary = data?.originalSizeBytes
    ? `旅游日记已发布，媒体优化完成：${formatSize(originalSizeBytes(data))} -> ${formatSize(compressedSizeBytes(data))}，${compressionText(data)}`
    : '旅游日记已发布'
  ElMessage({ type: 'success', message: compressionSummary, duration: 4600 })
  if (data?.id) {
    cacheDiary(data)
    diaries.value = [data, ...diaries.value.filter((item) => item.id !== data.id)]
    if (data.isPublic !== false) {
      hotDiaries.value = [data, ...hotDiaries.value.filter((item) => item.id !== data.id)].slice(0, 6)
    }
    cacheDiary(data)
    selectedDiaryId.value = data.id
    loadDiaryDetail(data.id)
  }
}

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('Failed to read image'))
    reader.readAsDataURL(file)
  })

const loadImage = (src) =>
  new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Failed to decode image'))
    image.src = src
  })

const normalizeImageForSharing = async (file) => {
  const originalDataUrl = await readFileAsDataUrl(file)
  const image = await loadImage(originalDataUrl)
  const scale = Math.min(1, DIARY_IMAGE_MAX_EDGE / Math.max(image.naturalWidth, image.naturalHeight))
  const width = Math.max(1, Math.round(image.naturalWidth * scale))
  const height = Math.max(1, Math.round(image.naturalHeight * scale))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) return originalDataUrl
  context.drawImage(image, 0, 0, width, height)
  return canvas.toDataURL('image/jpeg', DIARY_IMAGE_QUALITY)
}

const handleMediaFileChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  selectedMediaName.value = file.name
  form.value.mediaType = 'image'
  try {
    const normalizedDataUrl = await normalizeImageForSharing(file)
    form.value.mediaUrl = normalizedDataUrl
    form.value.originalSizeBytes = Math.round((normalizedDataUrl.length * 3) / 4)
    mediaPreview.value = normalizedDataUrl
  } catch (error) {
    console.error(error)
    selectedMediaName.value = ''
    form.value.mediaUrl = ''
    form.value.originalSizeBytes = null
    mediaPreview.value = ''
    ElMessage.error('图片处理失败，请换一张 JPG/PNG 图片重试')
  }
}

const handleMediaTypeChange = () => {
  selectedMediaName.value = ''
  mediaPreview.value = ''
  form.value.mediaUrl = ''
  form.value.originalSizeBytes = null
}

const fullText = async () => {
  if (!searchKeyword.value.trim()) {
    await load()
    return
  }

  loading.value = true
  try {
    const { data } = await searchDiaryFullText(searchKeyword.value.trim())
    diaries.value = Array.isArray(data) ? data : []
    seedDiaryCache(diaries.value)
    selectedDiaryId.value = diaries.value[0]?.id || null
    loadDiaryDetail(selectedDiaryId.value)
  } catch (error) {
    console.error(error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const selectDiary = async (diary) => {
  selectedDiaryId.value = diary.id
  cacheDiary(diary)
  loadDiaryDetail(diary.id)
  loadComments(diary)
}

const loadDiaryDetail = async (id) => {
  if (!id) return
  if (detailRequests.has(id)) return detailRequests.get(id)
  const request = getDiary(id)
    .then(({ data }) => {
      cacheDiary(data)
      return data
    })
    .finally(() => {
      detailRequests.delete(id)
    })
  detailRequests.set(id, request)
  return request
}

const removeCachedDiary = (id) => {
  diaryDetailCache.value.delete(id)
  commentCache.value.delete(id)
  delete comments.value[id]
  comments.value = { ...comments.value }
  const cachedImageUrl = diaryImageUrlCache.get(id)
  if (cachedImageUrl?.startsWith('blob:')) {
    URL.revokeObjectURL(cachedImageUrl)
  }
  diaryImageUrlCache.delete(id)
}

const removeDiary = async (diary) => {
  if (!diary?.id) return
  if (!canDeleteSelectedDiary.value) {
    ElMessage.warning('只能删除自己发布的日记')
    return
  }
  await ElMessageBox.confirm('确认删除这篇旅行日记吗？删除后不可恢复。', '删除日记', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const deletedId = diary.id
  await deleteDiary(deletedId)
  ElMessage.success('日记已删除')
  removeCachedDiary(deletedId)
  diaries.value = diaries.value.filter((item) => item.id !== deletedId)
  hotDiaries.value = hotDiaries.value.filter((item) => item.id !== deletedId)
  selectedDiaryId.value = diaries.value[0]?.id || null
  loadDiaryDetail(selectedDiaryId.value)
}

const interact = async (diary, type) => {
  const { data } = await interactDiary(diary.id, type)
  cacheDiary(data)
  ElMessage.success(type === 'share' ? '分享热度已更新' : '互动成功')
}

const loadComments = async (diary) => {
  if (!diary?.id) return
  if (commentCache.value.has(diary.id)) {
    comments.value = { ...comments.value, [diary.id]: commentCache.value.get(diary.id) }
    return
  }
  const { data } = await listDiaryComments(diary.id)
  const items = Array.isArray(data) ? data : []
  commentCache.value.set(diary.id, items)
  comments.value = { ...comments.value, [diary.id]: items }
}

const submitComment = async (diary) => {
  if (!diary?.id) return
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  await createDiaryComment(diary.id, {
    ...commentForm.value,
    authorName: commentForm.value.authorName.trim() || '游客',
    content: commentForm.value.content.trim(),
  })
  commentForm.value = { authorName: '游客', content: '' }
  commentCache.value.delete(diary.id)
  await loadComments(diary)
  await load()
}

const formatSize = (bytes) => {
  if (!bytes) return '0 MB'
  const size = Number(bytes)
  if (!Number.isFinite(size) || size <= 0) return '0 MB'
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const originalSizeBytes = (diary) => diary?.['originalSizeBytes'] ?? diary?.['original_size_bytes'] ?? 0

const compressedSizeBytes = (diary) => diary?.['compressedSizeBytes'] ?? diary?.['compressed_size_bytes'] ?? 0

const compressionText = (diary) => {
  if (diary?.compressionStatus === 'lossless_optimized') return '旧版模拟结果'
  const originalBytes = originalSizeBytes(diary)
  const optimizedBytes = compressedSizeBytes(diary)
  if (!originalBytes || !optimizedBytes) return '未上传媒体'
  const saved = 100 - (Number(optimizedBytes) / Number(originalBytes)) * 100
  return `节省 ${Math.max(0, saved).toFixed(0)}%`
}

const compressionSizeText = (diary) => `${formatSize(originalSizeBytes(diary))} -> ${formatSize(compressedSizeBytes(diary))}`

const compressionStatusText = (status) => {
  if (status === 'lossless_deflate') return 'DEFLATE 无损压缩'
  if (status === 'image_jpeg_optimized') return 'JPEG 图片优化'
  if (status === 'already_optimal') return '已是压缩格式'
  if (status === 'lossless_optimized') return '旧版模拟优化'
  return status || 'none'
}

const shareLink = (diary) => {
  if (!diary?.shareToken) return '生成中'
  return `${window.location.origin}${window.location.pathname}#/diaries?share=${diary.shareToken}`
}

const diaryCover = (diary) => (diary?.mediaType === 'image' && diary?.mediaUrl ? resolveApiAssetUrl(diary.mediaUrl) : diaryDefaultImage)

const revokeSelectedDiaryImage = () => {
  selectedDiaryImageUrl.value = ''
}

watch(
  () => [selectedDiary.value?.id, selectedDiary.value?.mediaUrl],
  async ([diaryId, mediaUrl]) => {
    revokeSelectedDiaryImage()
    if (!selectedDiary.value || selectedDiary.value.mediaType !== 'image' || !mediaUrl || !diaryId) {
      return
    }
    if (diaryImageUrlCache.has(diaryId)) {
      selectedDiaryImageUrl.value = diaryImageUrlCache.get(diaryId)
      return
    }
    try {
      const imageUrl = await fetchApiAssetBlobUrl(mediaUrl)
      diaryImageUrlCache.set(diaryId, imageUrl)
      if (selectedDiary.value?.id === diaryId) {
        selectedDiaryImageUrl.value = imageUrl
      }
    } catch (error) {
      console.error(error)
      selectedDiaryImageUrl.value = diaryDefaultImage
    }
  },
)

onMounted(load)
onBeforeUnmount(() => {
  diaryImageUrlCache.forEach((imageUrl) => {
    if (imageUrl?.startsWith('blob:')) URL.revokeObjectURL(imageUrl)
  })
  diaryImageUrlCache.clear()
  revokeSelectedDiaryImage()
})
</script>

<template>
  <section class="diary-page">
    <section class="diary-hero reveal-in">
      <div>
        <p class="section-kicker">Travel Diary</p>
        <h1>把旅行记忆变成可分享的动态游记</h1>
        <p>支持创作存储、媒体优化、AIGC 动画、热度评分和交流分享。</p>
      </div>
      <div class="hero-stats">
        <article>
          <strong>{{ diaries.length }}</strong>
          <span>已收录游记</span>
        </article>
        <article>
          <strong>{{ hotDiaries.length }}</strong>
          <span>热门内容</span>
        </article>
        <article>
          <strong>AIGC</strong>
          <span>动态回忆</span>
        </article>
      </div>
    </section>

    <section class="diary-layout">
      <el-card class="module-card composer-card reveal-in">
        <template #header>发布旅游日记</template>
        <el-form :model="form" label-width="110px">
          <el-row :gutter="12">
            <el-col :md="12" :xs="24">
              <el-form-item label="日记标题">
                <el-input v-model="form.title" placeholder="例如：海边黄昏的旅行记忆" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="媒体类型">
                <el-select v-model="form.mediaType" class="full-width" @change="handleMediaTypeChange">
                  <el-option label="文字" value="text" />
                  <el-option label="图片" value="image" />
                  <el-option label="视频" value="video" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="媒体素材">
            <div class="media-field">
              <el-upload
                v-if="form.mediaType === 'image'"
                accept="image/*"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleMediaFileChange"
              >
                <div class="upload-drop">
                  <UploadPicture theme="outline" size="28" fill="currentColor" />
                  <span>上传图片</span>
                  <small>选择本地照片后会自动生成预览，并用于压缩演示</small>
                </div>
              </el-upload>
              <el-input
                v-if="form.mediaType !== 'image' || !mediaPreview"
                v-model="form.mediaUrl"
                placeholder="也可粘贴图片或视频 URL，用于压缩与 AIGC 演示"
              />
              <div v-if="mediaPreview || selectedMediaName" class="media-preview">
                <img v-if="mediaPreview" :src="mediaPreview" alt="已选择的日记图片预览" />
                <div>
                  <strong>{{ selectedMediaName || '外部媒体地址' }}</strong>
                  <span>{{ form.originalSizeBytes ? formatSize(form.originalSizeBytes) : '发布后生成压缩对比' }}</span>
                </div>
              </div>
            </div>
          </el-form-item>
          <el-row :gutter="12">
            <el-col :md="12" :xs="24">
              <el-form-item label="用户评分">
                <el-input-number v-model="form.score" :min="0" :max="5" :step="0.1" class="full-width" />
              </el-form-item>
            </el-col>
            <el-col :md="12" :xs="24">
              <el-form-item label="公开分享">
                <el-switch v-model="form.isPublic" active-text="公开" inactive-text="私密" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="内容描述">
            <el-input v-model="form.content" type="textarea" :rows="5" placeholder="记录你的旅行故事、路线体验和推荐理由..." />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">
            <Send theme="outline" size="16" fill="currentColor" />
            发布并生成动态游记
          </el-button>
        </el-form>
      </el-card>

      <el-card class="module-card insight-card reveal-in">
        <template #header>热门游记</template>
        <div class="hot-list">
          <button
            v-for="item in hotDiaries"
            :key="item.id"
            type="button"
            class="hot-card"
            :class="{ active: selectedDiary?.id === item.id }"
            @click="selectDiary(item)"
          >
            <span><Fire theme="outline" size="14" fill="currentColor" /> 热度 {{ item.heatScore || 0 }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.views || 0 }} 浏览 / {{ item.likeCount || 0 }} 喜欢 / {{ item.shareCount || 0 }} 分享</small>
          </button>
          <el-empty v-if="!hotDiaries.length" description="暂无热门游记" />
        </div>
      </el-card>
    </section>

    <section class="diary-feed">
      <el-card class="module-card reveal-in">
        <div class="feed-tools">
          <el-input v-model="searchKeyword" placeholder="搜索标题或内容" clearable @keyup.enter="fullText" />
          <el-button type="primary" @click="fullText">
            <Search theme="outline" size="16" fill="currentColor" />
            搜索
          </el-button>
          <el-button @click="load">
            <Refresh theme="outline" size="16" fill="currentColor" />
            重置
          </el-button>
        </div>

        <div class="diary-content-grid">
          <aside class="diary-list">
            <button
              v-for="item in diaries"
              :key="item.id"
              type="button"
              class="diary-list-item"
              :class="{ active: selectedDiary?.id === item.id }"
              @click="selectDiary(item)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.content }}</span>
            </button>
            <el-empty v-if="!diaries.length" description="暂无日记数据" />
          </aside>

          <article v-if="selectedDiary" class="diary-panel">
            <div class="panel-header">
              <div>
                <p class="section-kicker">Selected Story</p>
                <h2>{{ selectedDiary.title }}</h2>
              </div>
              <span class="heat-badge"><Fire theme="outline" size="14" fill="currentColor" /> 热度 {{ selectedDiary.heatScore || 0 }}</span>
            </div>
            <p class="diary-story">{{ selectedDiary.content }}</p>

            <div class="story-media">
              <img :src="selectedDiaryImageUrl || diaryCover(selectedDiary)" :alt="selectedDiary.mediaUrl ? '日记图片' : '日记默认封面'" />
            </div>

            <div class="metadata-grid">
              <article>
                <span><Star theme="outline" size="15" fill="currentColor" /> 媒体优化</span>
                <strong>{{ compressionStatusText(selectedDiary.compressionStatus) }}</strong>
                <small>{{ compressionSizeText(selectedDiary) }} / {{ compressionText(selectedDiary) }}</small>
              </article>
              <article>
                <span><MagicWand theme="outline" size="15" fill="currentColor" /> AIGC 动画</span>
                <img class="metadata-cover" :src="aigcDefaultImage" alt="AIGC 动画占位图" loading="lazy" />
                <strong>{{ selectedDiary.aigcStatus || 'pending' }}</strong>
                <small>{{ selectedDiary.aigcAnimationUrl || '等待生成' }}</small>
              </article>
              <article>
                <span><Share theme="outline" size="15" fill="currentColor" /> 分享链接</span>
                <strong>
                  <Lock v-if="selectedDiary.isPublic === false" theme="outline" size="16" fill="currentColor" />
                  <Globe v-else theme="outline" size="16" fill="currentColor" />
                  {{ selectedDiary.isPublic === false ? '私密' : '公开' }}
                </strong>
                <small>{{ shareLink(selectedDiary) }}</small>
              </article>
            </div>

            <div class="diary-actions">
              <el-button type="primary" @click="interact(selectedDiary, 'like')">
                <Like theme="outline" size="16" fill="currentColor" />
                喜欢 {{ selectedDiary.likeCount || 0 }}
              </el-button>
              <el-button @click="interact(selectedDiary, 'favorite')">
                <Star theme="outline" size="16" fill="currentColor" />
                收藏 {{ selectedDiary.favoriteCount || 0 }}
              </el-button>
              <el-button @click="interact(selectedDiary, 'share')">
                <Share theme="outline" size="16" fill="currentColor" />
                分享 {{ selectedDiary.shareCount || 0 }}
              </el-button>
              <el-button @click="loadComments(selectedDiary)">
                <Message theme="outline" size="16" fill="currentColor" />
                查看评论 {{ selectedDiary.commentCount || 0 }}
              </el-button>
              <el-button v-if="canDeleteSelectedDiary" type="danger" @click="removeDiary(selectedDiary)">
                <Delete theme="outline" size="16" fill="currentColor" />
                删除日记
              </el-button>
            </div>

            <div class="comment-box">
              <div class="comment-form">
                <el-input v-model="commentForm.authorName" placeholder="昵称" />
                <el-input v-model="commentForm.content" placeholder="写下你的旅行交流..." />
                <el-button type="primary" @click="submitComment(selectedDiary)">
                  <Message theme="outline" size="16" fill="currentColor" />
                  评论
                </el-button>
              </div>
              <div class="comment-list">
                <article v-for="comment in comments[selectedDiary.id] || []" :key="comment.id">
                  <strong>{{ comment.authorName }}</strong>
                  <span>{{ comment.content }}</span>
                </article>
                <el-empty v-if="!(comments[selectedDiary.id] || []).length" description="暂无评论" />
              </div>
            </div>
          </article>
        </div>
      </el-card>
    </section>
  </section>
</template>

<style scoped>
.diary-page {
  display: grid;
  gap: 22px;
}

.diary-hero,
.diary-panel {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

.diary-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
}

.diary-hero h1 {
  margin-top: 8px;
  color: #f8fafc;
  font-size: 38px;
  line-height: 1.16;
  font-weight: 900;
}

.diary-hero p:last-child {
  margin-top: 10px;
  color: #a7b0bf;
  line-height: 1.7;
}

.section-kicker {
  color: #f3d08a;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.hero-stats {
  min-width: 320px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-stats article,
.metadata-grid article,
.hot-card,
.diary-list-item,
.comment-list article {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
}

.hero-stats article {
  padding: 16px;
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  color: #f8fafc;
  font-size: 24px;
  font-weight: 900;
}

.hero-stats span {
  margin-top: 8px;
  color: #a7b0bf;
  font-size: 12px;
}

.diary-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.full-width {
  width: 100%;
}

.media-field {
  width: 100%;
  display: grid;
  gap: 10px;
}

.media-field :deep(.el-upload) {
  width: 100%;
}

.upload-drop {
  width: 100%;
  min-height: 92px;
  display: grid;
  place-items: center;
  gap: 6px;
  padding: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.045);
  color: #f8fafc;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.upload-drop:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 56, 92, 0.7);
  background: rgba(255, 56, 92, 0.08);
}

.upload-drop span {
  font-weight: 900;
}

.upload-drop small,
.media-preview span {
  color: #a7b0bf;
  font-size: 12px;
}

.media-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}

.media-preview img {
  width: 78px;
  height: 58px;
  border-radius: 10px;
  object-fit: cover;
}

.media-preview strong,
.media-preview span {
  display: block;
}

.media-preview strong {
  color: #f8fafc;
  font-size: 14px;
}

.hot-list,
.diary-feed,
.diary-list,
.comment-list {
  display: grid;
  gap: 12px;
}

.hot-card,
.diary-list-item {
  width: 100%;
  padding: 16px;
  text-align: left;
  color: #f8fafc;
  cursor: pointer;
}

.hot-card.active,
.diary-list-item.active {
  border-color: rgba(255, 56, 92, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 56, 92, 0.12);
}

.hot-card span,
.hot-card small,
.diary-list-item span {
  display: block;
  color: #a7b0bf;
  font-size: 12px;
}

.hot-card strong,
.diary-list-item strong {
  display: block;
  margin: 8px 0;
  color: #f8fafc;
  font-size: 16px;
  line-height: 1.4;
}

.feed-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 16px;
}

.diary-content-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.4fr);
  gap: 18px;
}

.diary-list-item span {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.diary-panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-header h2 {
  margin-top: 6px;
  color: #f8fafc;
  font-size: 28px;
  line-height: 1.2;
}

.heat-badge {
  border-radius: 999px;
  background: rgba(255, 56, 92, 0.14);
  color: #ff8ba0;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.diary-story {
  margin-top: 18px;
  color: #d7dce5;
  line-height: 1.8;
}

.story-media {
  margin-top: 18px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.05);
}

.story-media img {
  display: block;
  width: 100%;
  max-height: 360px;
  object-fit: cover;
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.metadata-grid article {
  min-width: 0;
  padding: 14px;
}

.metadata-cover {
  width: 100%;
  aspect-ratio: 16 / 8;
  margin-top: 10px;
  border-radius: 10px;
  object-fit: cover;
}

.metadata-grid span,
.metadata-grid strong,
.metadata-grid small {
  display: block;
}

.metadata-grid span {
  color: #a7b0bf;
  font-size: 12px;
}

.metadata-grid strong {
  margin-top: 8px;
  color: #f8fafc;
  font-size: 15px;
}

.metadata-grid small {
  margin-top: 8px;
  color: #a7b0bf;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.diary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.comment-box {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.comment-form {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) auto;
  gap: 10px;
}

.comment-list article {
  padding: 12px;
}

.comment-list strong,
.comment-list span {
  display: block;
}

.comment-list strong {
  color: #f8fafc;
  font-size: 14px;
}

.comment-list span {
  margin-top: 4px;
  color: #a7b0bf;
  line-height: 1.6;
}

@media (max-width: 1000px) {
  .diary-hero,
  .diary-layout,
  .diary-content-grid,
  .metadata-grid {
    grid-template-columns: 1fr;
  }

  .diary-hero {
    display: grid;
  }

  .hero-stats {
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .feed-tools,
  .comment-form {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .diary-hero {
    padding: 24px;
  }

  .diary-hero h1 {
    font-size: 30px;
  }
}
</style>
