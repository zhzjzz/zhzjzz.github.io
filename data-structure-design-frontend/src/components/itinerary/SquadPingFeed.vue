<script setup>
import { makePingText } from '../../utils/itineraryVotes'

defineProps({
  events: { type: Array, default: () => [] },
})
</script>

<template>
  <section class="ping-feed">
    <h3>队友 Ping</h3>
    <div v-if="events.length" class="ping-list">
      <div v-for="event in events" :key="event.key || `${event.username}-${event.serverTimestamp}`" class="ping-item">
        <span>{{ event.username?.slice(0, 1) || '队' }}</span>
        <p>{{ event.text || makePingText(event.vote || event) }}</p>
      </div>
    </div>
    <div v-else class="ping-empty">暂无队友动作。</div>
  </section>
</template>

<style scoped>
.ping-feed {
  padding: 18px;
  border-radius: 20px;
  background: #17191d;
  color: #f8fafc;
}

.ping-feed h3 {
  margin: 0 0 12px;
}

.ping-list {
  display: grid;
  gap: 10px;
}

.ping-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
}

.ping-item span {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #ff385c;
  font-weight: 900;
}

.ping-item p {
  margin: 0;
  color: #d7dce5;
}

.ping-empty {
  color: #a7b0bf;
}
</style>
