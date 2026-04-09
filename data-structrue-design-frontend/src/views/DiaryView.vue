<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createDiary, listDiaries, searchDiaryFullText } from '../api/travel'

/**
 * 日记列表与检索状态。
 */
const diaries = ref([])
const loading = ref(false)
const searchKeyword = ref('')

/**
 * 日记创建表单。
 */
const form = ref({ title: '', content: '', mediaType: 'text', score: 4.5, views: 0 })

/**
 * 加载所有日记。
 */
const load = async () => {
  loading.value = true
  try {
    const { data } = await listDiaries()
    diaries.value = data
  } finally {
    loading.value = false
  }
}

/**
 * 提交新日记，发布后自动刷新列表。
 */
const submit = async () => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  await createDiary({ ...form.value, publishedAt: new Date().toISOString() })
  form.value = { title: '', content: '', mediaType: 'text', score: 4.5, views: 0 }
  ElMessage.success('日记发布成功')
  await load()
}

/**
 * 执行全文检索，便于快速定位攻略关键词内容。
 */
const fullText = async () => {
  if (!searchKeyword.value.trim()) {
    await load()
    return
  }
  loading.value = true
  try {
    const { data } = await searchDiaryFullText(searchKeyword.value)
    diaries.value = data.map((d) => ({ title: d.title, content: d.content, mediaType: 'search', score: '-', views: '-' }))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="diary-page">
    <el-card class="module-card diary-card">
      <div class="module-header">
        <div>
          <h2>旅游日记管理与交流</h2>
          <p class="module-subtitle">支持日记发布、全文检索与游记内容交流。</p>
        </div>
      </div>

      <el-form :model="form" label-width="110px">
        <el-row :gutter="12">
          <el-col :md="12" :xs="24">
            <el-form-item label="日记标题">
              <el-input v-model="form.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="媒体类型">
              <el-select v-model="form.mediaType" class="full-width">
                <el-option label="文字" value="text" />
                <el-option label="图片" value="image" />
                <el-option label="视频" value="video" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容描述">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="记录你的旅游故事..." />
        </el-form-item>
        <el-button type="primary" @click="submit">发布日记</el-button>
      </el-form>

      <el-divider />

      <el-row :gutter="12" class="search-row">
        <el-col :md="16" :xs="24">
          <el-input v-model="searchKeyword" placeholder="输入关键词进行全文检索" clearable @keyup.enter="fullText" />
        </el-col>
        <el-col :md="8" :xs="24" class="btn-group">
          <el-button type="primary" @click="fullText">搜索</el-button>
          <el-button @click="load">重置</el-button>
        </el-col>
      </el-row>

      <el-table :data="diaries" border stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="content" label="内容" min-width="380" show-overflow-tooltip />
        <el-table-column prop="mediaType" label="媒体类型" width="120" />
        <el-table-column prop="score" label="评分" width="100" />
        <el-table-column prop="views" label="浏览量" width="110" />
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.diary-card {
  background:
    radial-gradient(circle at 90% 15%, rgba(236, 72, 153, 0.16), transparent 22%),
    radial-gradient(circle at 8% 88%, rgba(99, 102, 241, 0.16), transparent 28%),
    rgba(255, 255, 255, 0.92);
}

.search-row {
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

.btn-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .btn-group {
    justify-content: flex-start;
    margin-top: 8px;
  }
}
</style>
