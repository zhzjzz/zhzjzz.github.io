<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Fire,
  Globe,
  Like,
  Lock,
  MagicWand,
  Message,
  Refresh,
  Search,
  Send,
  Share,
  Star,
  UploadPicture,
} from '@icon-park/vue-next'
import { useRoute } from 'vue-router'
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
const route = useRoute()
const showComposer = ref(false)
const composerRef = ref(null)
const detailRef = ref(null)
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

const openComposer = async () => {
  showComposer.value = true
  await nextTick()
  composerRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const closeComposer = () => {
  showComposer.value = false
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
    if (selectedDiaryId.value) {
      void loadDiaryDetail(selectedDiaryId.value)
      void loadComments(selectedDiary.value)
    }
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
  closeComposer()
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
    void loadDiaryDetail(data.id)
    void loadComments(data)
    await nextTick()
    detailRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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

const routeKeyword = () => {
  const keyword = route.query.keyword
  return Array.isArray(keyword) ? keyword[0] || '' : keyword || ''
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
    if (selectedDiaryId.value) {
      void loadDiaryDetail(selectedDiaryId.value)
      void loadComments(selectedDiary.value)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const applyRouteKeyword = async () => {
  const keyword = routeKeyword().trim()
  if (!keyword) {
    await load()
    return
  }
  searchKeyword.value = keyword
  await fullText()
}

const selectDiary = async (diary) => {
  selectedDiaryId.value = diary.id
  cacheDiary(diary)
  void loadDiaryDetail(diary.id)
  void loadComments(diary)
  await nextTick()
  detailRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
  if (selectedDiaryId.value) {
    void loadDiaryDetail(selectedDiaryId.value)
    void loadComments(selectedDiary.value)
  }
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

const originalSizeBytes = (diary) => diary?.originalSizeBytes ?? diary?.original_size_bytes ?? 0

const compressedSizeBytes = (diary) => diary?.compressedSizeBytes ?? diary?.compressed_size_bytes ?? 0

const compressionSizeText = (diary) => `${formatSize(originalSizeBytes(diary))} -> ${formatSize(compressedSizeBytes(diary))}`

const compressionText = (diary) => {
  if (diary?.compressionStatus === 'lossless_optimized') return '复刻图文压缩结果'
  const originalBytes = originalSizeBytes(diary)
  const optimizedBytes = compressedSizeBytes(diary)
  if (!originalBytes || !optimizedBytes) return '未上传媒体'
  const saved = 100 - (Number(optimizedBytes) / Number(originalBytes)) * 100
  return `节省 ${Math.max(0, saved).toFixed(0)}%`
}

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

const diaryAuthor = (diary) => diary?.authorName || appStore.user.name || '旅行者'

const diaryDestinationName = (diary) => diary?.destination?.name || '无指定目的地'

const diaryMediaBadge = (diary) => {
  if (diary?.mediaType === 'video') return '视频笔记'
  if (diary?.mediaType === 'image') return '图文笔记'
  return '纯文字'
}

const diaryTags = (diary) => {
  const tags = []
  if (diary?.destination?.name) tags.push(diary.destination.name)
  tags.push(diary?.isPublic === false ? '私密' : '公开')
  tags.push(diary?.mediaType === 'video' ? '视频' : diary?.mediaType === 'image' ? '图文' : '文字')
  return tags
}

const diaryExcerpt = (content) => {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  if (!text) return '还没有填写具体内容。'
  return text.length > 88 ? `${text.slice(0, 88)}…` : text
}

const formatPublishedAt = (value) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const diff = Date.now() - date.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))} 天前`
  return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}

const diaryCardRatio = (index) => {
  const ratios = ['4 / 5', '1 / 1', '5 / 4', '3 / 4']
  return ratios[index % ratios.length]
}

const selectedDiaryStats = computed(() => {
  const diary = selectedDiary.value
  if (!diary) return []
  return [
    { label: '浏览', value: diary.views || 0 },
    { label: '点赞', value: diary.likeCount || 0 },
    { label: '评论', value: diary.commentCount || 0 },
    { label: '收藏', value: diary.favoriteCount || 0 },
  ]
})

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

onMounted(applyRouteKeyword)
watch(() => route.query.keyword, applyRouteKeyword)
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
      <div class="hero-copy">
        <p class="section-kicker">Travel Diary · Xiaohongshu Flow</p>
        <h1>把旅行日记做成更像小红书的图文瀑布流</h1>
        <p class="module-subtitle">
          图文卡片、热门笔记、收藏与评论都保留，发布入口改成点击后再展开编辑。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="openComposer">
            <Send theme="outline" size="18" fill="currentColor" />
            发布旅游日记
          </el-button>
          <el-button size="large" @click="load">
            <Refresh theme="outline" size="18" fill="currentColor" />
            刷新内容
          </el-button>
        </div>
      </div>

      <div class="hero-stats">
        <article>
          <strong>{{ diaries.length }}</strong>
          <span>旅行笔记</span>
        </article>
        <article>
          <strong>{{ hotDiaries.length }}</strong>
          <span>热门推荐</span>
        </article>
        <article>
          <strong>AIGC</strong>
          <span>动态回忆</span>
        </article>
      </div>
    </section>

    <section class="diary-stage">
      <div class="diary-main-column">
        <section ref="composerRef" class="composer-anchor reveal-in">
          <button v-if="!showComposer" type="button" class="composer-trigger" @click="openComposer">
            <span class="composer-trigger__icon">
              <Send theme="outline" size="18" fill="currentColor" />
            </span>
            <span>
              <strong>发布旅游日记</strong>
              <small>点开后再展开编辑，不占首屏位置</small>
            </span>
          </button>

          <div v-else class="composer-card">
            <div class="composer-card__header">
              <div>
                <p class="section-kicker">Publish Note</p>
                <h2>发布旅游日记</h2>
              </div>
              <el-button text @click="closeComposer">收起</el-button>
            </div>

            <el-form :model="form" label-position="top" class="composer-form">
              <div class="composer-grid">
                <el-form-item label="日记标题">
                  <el-input v-model="form.title" placeholder="例如：海边黄昏的旅行记忆" />
                </el-form-item>
                <el-form-item label="媒体类型">
                  <el-select v-model="form.mediaType" class="full-width" @change="handleMediaTypeChange">
                    <el-option label="文字" value="text" />
                    <el-option label="图片" value="image" />
                    <el-option label="视频" value="video" />
                  </el-select>
                </el-form-item>
              </div>

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
                    placeholder="也可以粘贴图片或视频 URL，后续用于压缩与 AIGC 演示"
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

              <div class="composer-grid">
                <el-form-item label="用户评分">
                  <el-input-number v-model="form.score" :min="0" :max="5" :step="0.1" class="full-width" />
                </el-form-item>
                <el-form-item label="公开分享">
                  <el-switch v-model="form.isPublic" active-text="公开" inactive-text="私密" />
                </el-form-item>
              </div>

              <el-form-item label="内容描述">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="5"
                  placeholder="记录你的旅行故事、路线体验和推荐理由..."
                />
              </el-form-item>

              <div class="composer-actions">
                <el-button @click="closeComposer">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="submit">
                  <Send theme="outline" size="16" fill="currentColor" />
                  发布笔记
                </el-button>
              </div>
            </el-form>
          </div>
        </section>

        <section class="feed-toolbar reveal-in">
          <div class="toolbar-copy">
            <p class="section-kicker">Discovery Feed</p>
            <h2>灵感流</h2>
            <span>{{ diaries.length }} 篇日记</span>
          </div>
          <div class="toolbar-actions">
            <el-input v-model="searchKeyword" class="search-input" placeholder="搜索标题或内容" clearable @keyup.enter="fullText" />
            <el-button type="primary" @click="fullText">
              <Search theme="outline" size="16" fill="currentColor" />
              搜索
            </el-button>
            <el-button @click="load">
              <Refresh theme="outline" size="16" fill="currentColor" />
              重置
            </el-button>
          </div>
        </section>

        <section class="diary-flow reveal-in" v-loading="loading">
          <button
            v-for="(item, index) in diaries"
            :key="item.id"
            type="button"
            class="note-card"
            :class="{ active: selectedDiary?.id === item.id }"
            @click="selectDiary(item)"
          >
            <div class="note-media" :style="{ '--card-ratio': diaryCardRatio(index) }">
              <img :src="diaryCover(item)" :alt="item.title || '旅游日记封面'" loading="lazy" />
              <div class="note-media__overlay">
                <span class="note-badge">{{ diaryMediaBadge(item) }}</span>
                <span class="note-badge note-badge--soft">{{ diaryDestinationName(item) }}</span>
              </div>
            </div>
            <div class="note-body">
              <div class="note-topline">
                <span class="note-author">{{ diaryAuthor(item) }}</span>
                <span class="note-time">{{ formatPublishedAt(item.publishedAt) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ diaryExcerpt(item.content) }}</p>
              <div class="note-tags">
                <span v-for="tag in diaryTags(item)" :key="tag" class="note-chip">{{ tag }}</span>
              </div>
              <div class="note-stats">
                <span><Like theme="outline" size="14" fill="currentColor" /> {{ item.likeCount || 0 }}</span>
                <span><Message theme="outline" size="14" fill="currentColor" /> {{ item.commentCount || 0 }}</span>
                <span><Share theme="outline" size="14" fill="currentColor" /> {{ item.shareCount || 0 }}</span>
              </div>
            </div>
          </button>
          <el-empty v-if="!diaries.length && !loading" description="暂无日记内容" />
        </section>
      </div>

      <aside class="diary-side-column">
        <div class="side-panel side-panel--hot reveal-in">
          <div class="panel-heading">
            <div>
              <p class="section-kicker">Hot Picks</p>
              <h2>热门笔记</h2>
            </div>
            <span>{{ hotDiaries.length }}</span>
          </div>
          <div class="hot-note-list">
            <button
              v-for="item in hotDiaries"
              :key="item.id"
              type="button"
              class="hot-note-card"
              :class="{ active: selectedDiary?.id === item.id }"
              @click="selectDiary(item)"
            >
              <img :src="diaryCover(item)" :alt="item.title || '热门日记封面'" loading="lazy" />
              <div class="hot-note-card__body">
                <strong>{{ item.title }}</strong>
                <small>{{ diaryDestinationName(item) }} · {{ formatPublishedAt(item.publishedAt) }}</small>
              </div>
              <span class="heat-pill">
                <Fire theme="outline" size="14" fill="currentColor" />
                {{ Math.round(item.heatScore || 0) }}
              </span>
            </button>
            <el-empty v-if="!hotDiaries.length" description="暂无热门日记" />
          </div>
        </div>

        <article v-if="selectedDiary" ref="detailRef" class="detail-panel reveal-in">
          <div class="detail-hero">
            <div class="detail-title">
              <p class="section-kicker">Selected Story</p>
              <h2>{{ selectedDiary.title }}</h2>
              <div class="detail-meta">
                <span>{{ diaryAuthor(selectedDiary) }}</span>
                <span>{{ formatPublishedAt(selectedDiary.publishedAt) }}</span>
                <span>{{ diaryDestinationName(selectedDiary) }}</span>
              </div>
            </div>
            <span class="heat-badge">
              <Fire theme="outline" size="14" fill="currentColor" />
              热度 {{ Math.round(selectedDiary.heatScore || 0) }}
            </span>
          </div>

          <div class="detail-cover">
            <img
              :src="selectedDiaryImageUrl || diaryCover(selectedDiary)"
              :alt="selectedDiary.mediaUrl ? '日记图片' : '日记默认封面'"
            />
          </div>

          <p class="detail-story">{{ selectedDiary.content }}</p>

          <div class="detail-badges">
            <span v-for="tag in diaryTags(selectedDiary)" :key="tag">{{ tag }}</span>
            <span>{{ compressionStatusText(selectedDiary.compressionStatus) }}</span>
          </div>

          <div class="stats-grid">
            <article v-for="item in selectedDiaryStats" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </article>
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
              点赞 {{ selectedDiary.likeCount || 0 }}
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
              评论 {{ selectedDiary.commentCount || 0 }}
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
      </aside>
    </section>
  </section>
</template>

<style scoped>
.diary-page {
  --el-color-primary: #ff385c;
  --el-fill-color-blank: #17191d;
  --el-text-color-primary: #f8fafc;
  --el-text-color-regular: #a7b0bf;
  --el-border-color: rgba(255, 255, 255, 0.12);
  --el-border-radius-base: 8px;
  color: #f8fafc;
  display: grid;
  gap: 18px;
  padding: 6px 0 10px;
  background:
    linear-gradient(135deg, rgba(255, 56, 92, 0.11), transparent 30%),
    linear-gradient(180deg, rgba(23, 25, 29, 0.96), #0d0f12 56%);
}

.diary-hero,
.composer-card,
.composer-trigger,
.feed-toolbar,
.note-card,
.side-panel,
.hot-note-card,
.detail-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(23, 25, 29, 0.86);
  box-shadow: 0 22px 70px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(18px);
}

.diary-hero::before,
.composer-card::before,
.composer-trigger::before,
.feed-toolbar::before,
.note-card::before,
.side-panel::before,
.hot-note-card::before,
.detail-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(120deg, transparent 8%, rgba(255, 255, 255, 0.16) 18%, transparent 30%);
  opacity: 0;
  transform: translateX(-80%);
}

.diary-hero:hover::before,
.composer-card:hover::before,
.composer-trigger:hover::before,
.feed-toolbar:hover::before,
.note-card:hover::before,
.side-panel:hover::before,
.hot-note-card:hover::before,
.detail-panel:hover::before {
  animation: feedSheen 960ms var(--motion-ease);
}

.diary-hero {
  display: flex;
  justify-content: space-between;
  gap: 22px;
  padding: 28px;
}

.hero-copy {
  max-width: 720px;
}

.diary-hero h1 {
  margin-top: 8px;
  color: #f8fafc;
  font-size: clamp(30px, 3vw, 44px);
  line-height: 1.08;
  font-weight: 900;
  letter-spacing: 0;
}

.module-subtitle {
  margin-top: 12px;
  color: #a7b0bf;
  font-size: 15px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-stats {
  min-width: 290px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article,
.stats-grid article,
.metadata-grid article,
.note-chip,
.detail-badges span {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
}

.hero-stats article {
  padding: 16px;
  border-radius: 18px;
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  color: #ff8ba0;
  font-size: 24px;
  font-weight: 900;
}

.hero-stats span {
  margin-top: 8px;
  color: #a7b0bf;
  font-size: 12px;
}

.diary-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 18px;
  align-items: start;
}

.diary-main-column,
.diary-side-column {
  display: grid;
  gap: 16px;
}

.composer-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  text-align: left;
  cursor: pointer;
  transition: transform 220ms var(--motion-spring), box-shadow 220ms ease, border-color 220ms ease;
}

.composer-trigger:hover,
.composer-trigger:focus-visible {
  transform: translateY(-4px);
  border-color: rgba(255, 56, 92, 0.36);
  box-shadow: 0 34px 90px rgba(0, 0, 0, 0.42), 0 0 0 1px rgba(255, 56, 92, 0.12);
}

.composer-trigger__icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(135deg, #ff385c, #f3d08a);
  color: #ffffff;
  box-shadow: 0 16px 36px rgba(255, 56, 92, 0.24);
}

.composer-trigger strong {
  display: block;
  color: #f8fafc;
  font-size: 16px;
  font-weight: 900;
}

.composer-trigger small {
  display: block;
  margin-top: 4px;
  color: #a7b0bf;
  font-size: 12px;
}

.composer-card {
  padding: 22px;
}

.composer-card__header,
.panel-heading,
.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.composer-card__header h2,
.panel-heading h2,
.detail-title h2 {
  margin-top: 6px;
  color: #f8fafc;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 900;
  letter-spacing: 0;
}

.composer-form {
  margin-top: 12px;
}

.composer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
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
  min-height: 104px;
  display: grid;
  place-items: center;
  gap: 6px;
  padding: 18px;
  border: 1px dashed rgba(255, 36, 66, 0.28);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.045);
  color: #f8fafc;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.upload-drop:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 56, 92, 0.58);
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
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
}

.media-preview img {
  width: 82px;
  height: 64px;
  border-radius: 14px;
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

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.feed-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
}

.toolbar-copy h2 {
  margin-top: 6px;
  color: #f8fafc;
  font-size: 28px;
  line-height: 1.18;
  font-weight: 900;
}

.toolbar-copy span {
  display: inline-flex;
  margin-top: 6px;
  color: #a7b0bf;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  width: min(420px, 100%);
}

.diary-flow {
  column-count: 2;
  column-gap: 16px;
  padding-top: 8px;
}

.note-card {
  width: 100%;
  display: grid;
  gap: 0;
  margin: 0 0 16px;
  padding: 0;
  overflow: hidden;
  text-align: left;
  cursor: pointer;
  break-inside: avoid;
  transform: translateZ(0);
  transition: transform 220ms var(--motion-spring), box-shadow 220ms ease, border-color 220ms ease;
}

.note-card:nth-child(2n) {
  margin-top: 28px;
}

.note-card:nth-child(3n) {
  margin-top: 14px;
}

.note-card:hover,
.note-card:focus-visible,
.hot-note-card:hover,
.hot-note-card:focus-visible {
  transform: translateY(-8px) scale(1.01);
  border-color: rgba(255, 56, 92, 0.36);
  box-shadow: 0 34px 90px rgba(0, 0, 0, 0.42), 0 0 0 1px rgba(255, 56, 92, 0.12);
}

.note-card.active,
.hot-note-card.active {
  border-color: rgba(255, 56, 92, 0.42);
  box-shadow: 0 0 0 3px rgba(255, 56, 92, 0.08), 0 30px 80px rgba(255, 56, 92, 0.12);
}

.note-media {
  position: relative;
  aspect-ratio: var(--card-ratio, 4 / 5);
  overflow: hidden;
  background: #24272d;
}

.note-media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 520ms ease, filter 520ms ease;
}

.note-card:hover .note-media img {
  transform: scale(1.04);
  filter: saturate(1.08) contrast(1.02);
}

.note-media::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, transparent 48%, rgba(255, 56, 92, 0.14) 50%, transparent 52%),
    linear-gradient(180deg, transparent, rgba(13, 15, 18, 0.72));
  opacity: 0;
  transform: translateY(-18%);
}

.note-card:hover .note-media::after {
  animation: scanLine 920ms var(--motion-ease);
}

.note-media__overlay {
  position: absolute;
  inset: auto 12px 12px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.note-badge,
.heat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.note-badge {
  color: #ffffff;
  background: rgba(17, 24, 39, 0.76);
  backdrop-filter: blur(10px);
}

.note-badge--soft {
  background: rgba(13, 15, 18, 0.72);
  color: #ff8ba0;
}

.note-body {
  display: grid;
  gap: 10px;
  padding: 16px 16px 18px;
}

.note-topline,
.note-stats,
.detail-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.note-topline {
  justify-content: space-between;
  color: #a7b0bf;
  font-size: 12px;
}

.note-body h3 {
  color: #f8fafc;
  font-size: 18px;
  line-height: 1.28;
  font-weight: 900;
  letter-spacing: 0;
}

.note-body p {
  color: #d7dce5;
  font-size: 14px;
  line-height: 1.7;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.note-chip,
.detail-badges span {
  padding: 6px 10px;
  border-radius: 999px;
  color: #f8fafc;
  font-size: 12px;
  font-weight: 700;
}

.note-stats {
  justify-content: space-between;
  color: #a7b0bf;
  font-size: 12px;
}

.note-stats span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.side-panel,
.detail-panel {
  padding: 20px;
}

.panel-heading span,
.heat-pill {
  color: #ff8ba0;
}

.hot-note-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.hot-note-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 10px;
  text-align: left;
  cursor: pointer;
  transition: transform 220ms var(--motion-spring), box-shadow 220ms ease, border-color 220ms ease;
}

.hot-note-card img {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  object-fit: cover;
}

.hot-note-card__body {
  min-width: 0;
}

.hot-note-card__body strong {
  display: block;
  color: #f8fafc;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 800;
}

.hot-note-card__body small {
  display: block;
  margin-top: 4px;
  color: #a7b0bf;
  font-size: 12px;
}

.heat-pill {
  justify-self: end;
  background: rgba(255, 56, 92, 0.12);
}

.detail-panel {
  display: grid;
  gap: 16px;
}

.detail-hero {
  align-items: flex-start;
}

.detail-title {
  min-width: 0;
}

.detail-meta {
  margin-top: 10px;
  color: #a7b0bf;
  font-size: 12px;
}

.heat-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 56, 92, 0.12);
  color: #ff8ba0;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.detail-cover {
  overflow: hidden;
  border-radius: 20px;
}

.detail-cover img {
  display: block;
  width: 100%;
  max-height: 360px;
  object-fit: cover;
}

.detail-story {
  color: #d7dce5;
  line-height: 1.8;
}

.detail-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stats-grid article,
.metadata-grid article,
.comment-list article {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
}

.stats-grid strong {
  display: block;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 900;
}

.stats-grid span {
  display: block;
  margin-top: 6px;
  color: #a7b0bf;
  font-size: 12px;
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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

.metadata-cover {
  width: 100%;
  aspect-ratio: 16 / 8;
  margin-top: 10px;
  border-radius: 14px;
  object-fit: cover;
}

.diary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.comment-box {
  display: grid;
  gap: 12px;
}

.comment-form {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) auto;
  gap: 10px;
}

.comment-list {
  display: grid;
  gap: 10px;
}

.comment-list article {
  padding: 12px 14px;
}

.comment-list strong,
.comment-list span {
  display: block;
}

.comment-list strong {
  color: #f8fafc;
  font-size: 14px;
  font-weight: 800;
}

.comment-list span {
  margin-top: 4px;
  color: #d7dce5;
  line-height: 1.6;
}

@media (max-width: 1120px) {
  .diary-stage {
    grid-template-columns: 1fr;
  }

  .diary-flow {
    column-count: 2;
  }
}

@media (max-width: 860px) {
  .diary-hero,
  .feed-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-stats,
  .composer-grid,
  .metadata-grid,
  .stats-grid,
  .comment-form {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .diary-page {
    gap: 14px;
  }

  .diary-hero,
  .composer-card,
  .feed-toolbar,
  .side-panel,
  .detail-panel {
    padding: 18px;
  }

  .diary-flow {
    column-count: 1;
  }

  .note-card:nth-child(n) {
    margin-top: 0;
  }

  .note-body h3 {
    font-size: 17px;
  }

  .toolbar-copy h2 {
    font-size: 24px;
  }
}

@keyframes feedSheen {
  0% {
    opacity: 0;
    transform: translateX(-80%);
  }
  24%,
  60% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(80%);
  }
}

@keyframes scanLine {
  0% {
    opacity: 0;
    transform: translateY(-22%);
  }
  30%,
  68% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(22%);
  }
}
</style>
