<script setup>
import { onMounted, ref } from 'vue'
import { createItinerary, listItineraries } from '../api/travel'

const list = ref([])
const form = ref({ name: '', owner: '演示用户', collaborators: '好友A,好友B', strategy: 'time', transportMode: 'walk+shuttle', notes: '' })

const load = async () => {
  const { data } = await listItineraries()
  list.value = data
}

const submit = async () => {
  await createItinerary(form.value)
  form.value = { name: '', owner: '演示用户', collaborators: '好友A,好友B', strategy: 'time', transportMode: 'walk+shuttle', notes: '' }
  await load()
}

onMounted(load)
</script>

<template>
  <section class="card">
    <h2>协作行程</h2>
    <div class="grid">
      <input v-model="form.name" placeholder="行程名称" />
      <input v-model="form.owner" placeholder="创建人" />
      <input v-model="form.collaborators" placeholder="协作人（逗号分隔）" />
      <input v-model="form.strategy" placeholder="策略" />
      <input v-model="form.transportMode" placeholder="交通模式" />
      <textarea v-model="form.notes" placeholder="备注"></textarea>
      <button @click="submit">创建行程</button>
    </div>

    <table>
      <thead><tr><th>名称</th><th>创建人</th><th>协作人</th><th>策略</th><th>交通</th></tr></thead>
      <tbody>
        <tr v-for="item in list" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.owner }}</td>
          <td>{{ item.collaborators }}</td>
          <td>{{ item.strategy }}</td>
          <td>{{ item.transportMode }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.card { background: #fff; padding: 16px; border-radius: 8px; }
.grid { display: grid; gap: 8px; margin-bottom: 12px; }
textarea { min-height: 80px; }
input,textarea,button { padding: 6px 8px; }
table { width: 100%; border-collapse: collapse; }
th, td { border-bottom: 1px solid #e5e7eb; text-align: left; padding: 8px; }
</style>
