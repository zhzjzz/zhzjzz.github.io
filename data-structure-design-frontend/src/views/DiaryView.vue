<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Fire, Globe, Like, Lock, MagicWand, Message, Refresh, Search, Send, Share, Star, UploadPicture } from '@icon-park/vue-next'
import {
  createDiary,
  createDiaryComment,
  interactDiary,
  listDiaries,
  listDiaryComments,
  listHotDiaries,
  searchDiaryFullText,
} from '../api/travel'
import diaryDefaultImage from '../assets/defaults/diary-default.png'
import aigcDefaultImage from '../assets/defaults/aigc-animation-default.png'

const diaries = ref([])
const hotDiaries = ref([])
const comments = ref({})
const loading = ref(false)
const searchKeyword = ref('')
const selectedDiaryId = ref(null)
const selectedMediaName = ref('')
const mediaPreview = ref('')
const commentForm = ref({ authorName: '游客', content: '' })
const MAX_DIARY_IMAGE_WIDTH = 1280
const DIARY_IMAGE_QUALITY = 0.78
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

const selectedDiary = computed(() => diaries.value.find((item) => item.id === selectedDiaryId.value) || diaries.value[0])

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
    selectedDiaryId.value = diaries.value.some((item) => item.id === previousId) ? previousId : diaries.value[0]?.id || null
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
  const { data } = await createDiary(payload)
  resetForm()
  const compressionSummary = data?.originalSizeBytes
    ? `旅游日记已发布，媒体优化完成：${formatSize(data.originalSizeBytes)} -> ${formatSize(data.compressedSizeBytes)}，${compressionText(data)}`
    : '旅游日记已发布'
  ElMessage({ type: 'success', message: compressionSummary, duration: 4600 })
  await load()
}

const handleMediaFileChange = (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  selectedMediaName.value = file.name
  form.value.mediaType = 'image'
  form.value.originalSizeBytes = file.size

  const reader = new FileReader()
  reader.onload = () => {
    compressImage(String(reader.result || ''), file.type)
      .then((url) => {
        form.value.mediaUrl = url
        mediaPreview.value = url
      })
      .catch(() => {
        form.value.mediaUrl = String(reader.result || '')
        mediaPreview.value = form.value.mediaUrl
      })
  }
  reader.onerror = () => ElMessage.error('图片读取失败，请重新选择')
  reader.readAsDataURL(file)
}

const compressImage = (dataUrl, mimeType = 'image/jpeg') =>
  new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const scale = Math.min(1, MAX_DIARY_IMAGE_WIDTH / image.width)
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(image.width * scale))
      canvas.height = Math.max(1, Math.round(image.height * scale))
      const context = canvas.getContext('2d')
      if (!context) {
        reject(new Error('canvas unavailable'))
        return
      }
      context.drawImage(image, 0, 0, canvas.width, canvas.height)
      const outputType = mimeType === 'image/png' ? 'image/png' : 'image/jpeg'
      resolve(canvas.toDataURL(outputType, DIARY_IMAGE_QUALITY))
    }
    image.onerror = reject
    image.src = dataUrl
  })

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
    const { data } = await searchDiaryFullText(searchKeyword.value)
    diaries.value = Array.isArray(data) ? data : []
    selectedDiaryId.value = diaries.value[0]?.id || null
  } finally {
    loading.value = false
  }
}

const selectDiary = async (diary) => {
  selectedDiaryId.value = diary.id
  await loadComments(diary)
}

const interact = async (diary, type) => {
  const { data } = await interactDiary(diary.id, type)
  const index = diaries.value.findIndex((item) => item.id === data.id)
  if (index >= 0) diaries.value[index] = data
  const hotIndex = hotDiaries.value.findIndex((item) => item.id === data.id)
  if (hotIndex >= 0) hotDiaries.value[hotIndex] = data
  ElMessage.success(type === 'share' ? '分享热度已更新' : '互动成功')
}

const loadComments = async (diary) => {
  if (!diary?.id) return
  const { data } = await listDiaryComments(diary.id)
  comments.value = { ...comments.value, [diary.id]: Array.isArray(data) ? data : [] }
}

const submitComment = async (diary) => {
  if (!diary?.id) return
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  await createDiaryComment(diary.id, commentForm.value)
  commentForm.value = { authorName: '游客', content: '' }
  await loadComments(diary)
  await load()
}

const formatSize = (bytes) => {
  if (!bytes) return '0 MB'
  return `${(Number(bytes) / 1024 / 1024).toFixed(1)} MB`
}

const compressionText = (diary) => {
  if (!diary?.originalSizeBytes || !diary?.compressedSizeBytes) return '未上传媒体'
  const saved = 100 - (Number(diary.compressedSizeBytes) / Number(diary.originalSizeBytes)) * 100
  return `节省 ${Math.max(0, saved).toFixed(0)}%`
}

const shareLink = (diary) => {
  if (!diary?.shareToken) return '生成中'
  return `${window.location.origin}${window.location.pathname}#/diaries?share=${diary.shareToken}`
}

const diaryCover = (diary) => (diary?.mediaType === 'image' && diary?.mediaUrl ? diary.mediaUrl : diaryDefaultImage)

onMounted(load)
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
          <el-button type="primary" @click="submit">
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
            <small>{{ item.views || 0 }} 浏览 · {{ item.likeCount || 0 }} 喜欢 · {{ item.shareCount || 0 }} 分享</small>
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

        <div class="diary-content-grid" v-loading="loading">
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
              <img :src="diaryCover(selectedDiary)" :alt="selectedDiary.mediaUrl ? '日记图片' : '日记默认封面'" />
            </div>

            <div class="metadata-grid">
              <article>
                <span><Star theme="outline" size="15" fill="currentColor" /> 媒体优化</span>
                <strong>{{ selectedDiary.compressionStatus || 'none' }}</strong>
                <small>{{ formatSize(selectedDiary.originalSizeBytes) }} -> {{ formatSize(selectedDiary.compressedSizeBytes) }} · {{ compressionText(selectedDiary) }}</small>
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
