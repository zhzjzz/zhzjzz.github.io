<script setup>
import { onMounted, ref } from 'vue'
import { createDiary, listDiaries, searchDiaryFullText } from '../api/travel'

const diaries = ref([])
const searchKeyword = ref('')
const form = ref({ title: '', content: '', mediaType: 'text', score: 4.5, views: 0 })

const load = async () => {
  const { data } = await listDiaries()
  diaries.value = data
}

const submit = async () => {
  await createDiary({ ...form.value, publishedAt: new Date().toISOString() })
  form.value = { title: '', content: '', mediaType: 'text', score: 4.5, views: 0 }
  await load()
}

const fullText = async () => {
  if (!searchKeyword.value) return load()
  const { data } = await searchDiaryFullText(searchKeyword.value)
  diaries.value = data.map((d) => ({ title: d.title, content: d.content, score: '-', views: '-' }))
}

onMounted(load)
</script>

<template>
  <section class="card">
    <h2>旅游日记</h2>
    <div class="form-grid">
      <input v-model="form.title" placeholder="标题" />
      <input v-model="form.mediaType" placeholder="媒体类型（text/image/video）" />
      <textarea v-model="form.content" placeholder="内容"></textarea>
      <button @click="submit">发布日记</button>
    </div>

    <div class="toolbar">
      <input v-model="searchKeyword" placeholder="全文检索关键词" />
      <button @click="fullText">搜索</button>
      <button @click="load">重置</button>
    </div>

    <ul>
      <li v-for="(d, idx) in diaries" :key="idx">
        <strong>{{ d.title }}</strong> - {{ d.content }}
      </li>
    </ul>
  </section>
</template>

<style scoped>
.card { background: #fff; padding: 16px; border-radius: 8px; }
.form-grid { display: grid; gap: 8px; margin-bottom: 12px; }
textarea { min-height: 90px; }
.toolbar { display: flex; gap: 8px; margin: 10px 0; }
input,textarea,button { padding: 6px 8px; }
</style>
