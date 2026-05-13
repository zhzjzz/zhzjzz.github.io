<script setup>
import { computed, ref, watch } from 'vue'
import { voteTypeLabel, voteTypeTone, VOTE_TYPES } from '../../utils/itineraryVotes'

const props = defineProps({
  node: { type: Object, default: null },
  currentUser: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits(['submit-vote'])
const reason = ref('')

const myVote = computed(() => props.node?.votes?.find((vote) => vote.username === props.currentUser))

watch(() => props.node?.spotId, () => {
  reason.value = myVote.value?.reason || ''
})

const submit = (voteType) => {
  if (!props.node) return
  emit('submit-vote', {
    spotId: props.node.spotId,
    spotName: props.node.spotName,
    voteType,
    reason: reason.value,
  })
}
</script>

<template>
  <aside class="decision-card">
    <template v-if="node">
      <div class="decision-head">
        <span>景点决策卡</span>
        <strong>{{ node.spotName }}</strong>
        <small>当前状态：{{ voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus) }}</small>
      </div>
      <el-input v-model="reason" type="textarea" :rows="3" placeholder="补一句理由，例如：夜景必看、太绕路、适合备选" />
      <div class="vote-buttons">
        <el-button
          v-for="type in VOTE_TYPES"
          :key="type"
          :type="voteTypeTone(type)"
          :loading="saving"
          @click="submit(type)"
        >
          {{ voteTypeLabel(type) }}
        </el-button>
      </div>
      <div class="vote-list">
        <div v-for="vote in node.votes" :key="`${vote.username}-${vote.voteType}`" class="vote-row">
          <strong>{{ vote.username }}</strong>
          <span>{{ voteTypeLabel(vote.voteType) }}</span>
          <small>{{ vote.reason || '没有填写理由' }}</small>
        </div>
      </div>
    </template>
    <div v-else class="decision-empty">选择一个地图节点后查看投票。</div>
  </aside>
</template>

<style scoped>
.decision-card {
  min-height: 100%;
  padding: 18px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.22);
}

.decision-head span,
.decision-head strong,
.decision-head small {
  display: block;
}

.decision-head span {
  color: #ff385c;
  font-size: 12px;
  font-weight: 900;
}

.decision-head strong {
  margin-top: 6px;
  color: #222222;
  font-size: 22px;
}

.decision-head small {
  margin: 6px 0 14px;
  color: #64748b;
}

.vote-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.vote-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.vote-row {
  display: grid;
  gap: 3px;
  padding: 10px;
  border-radius: 12px;
  background: #f8fafc;
}

.vote-row strong {
  color: #222222;
}

.vote-row span {
  color: #ff385c;
  font-weight: 800;
}

.vote-row small {
  color: #64748b;
}

.decision-empty {
  display: grid;
  min-height: 220px;
  place-items: center;
  color: #64748b;
  font-weight: 800;
}
</style>
