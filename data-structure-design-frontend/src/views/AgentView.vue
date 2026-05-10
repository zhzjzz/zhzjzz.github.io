<script setup>
import { computed, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bowl,
  MapDraw,
  MapRoad,
  NotebookAndPen,
  Robot,
  Send,
  Shop,
} from '@icon-park/vue-next'
import { chatWithTravelAgent } from '../api/travel'

const loading = ref(false)
const input = ref('')
const messageListRef = ref(null)
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是旅游服务 Agent。你可以问我目的地推荐、路线解释、周边设施、美食、日记创作或行程建议。',
    fallback: false,
  },
])

const quickPrompts = [
  { label: '推荐校园参观点', icon: MapDraw, text: '我想参观校园，请帮我推荐几个适合第一次来的目的地。' },
  { label: '解释路线规划', icon: MapRoad, text: '如果我要查询具体路线，系统会怎样帮我规划？' },
  { label: '查周边设施', icon: Shop, text: '我到了一个景点附近，怎么查询周边设施？' },
  { label: '推荐美食', icon: Bowl, text: '帮我想想在校园或景区附近怎么找美食。' },
  { label: '日记建议', icon: NotebookAndPen, text: '我想写一篇旅游日记，可以给我一些创作建议吗？' },
]

const canSend = computed(() => input.value.trim().length > 0 && !loading.value)

const scrollToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

const buildHistory = () =>
  messages.value
    .filter((message) => ['user', 'assistant'].includes(message.role))
    .slice(-8)
    .map((message) => ({
      role: message.role,
      content: message.content,
    }))

const sendMessage = async (text = input.value) => {
  const content = text.trim()
  if (!content || loading.value) return

  const history = buildHistory()
  messages.value.push({ role: 'user', content })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const { data } = await chatWithTravelAgent({ message: content, history })
    messages.value.push({
      role: 'assistant',
      content: data?.reply || '暂时没有拿到有效回复，请稍后再试。',
      fallback: Boolean(data?.fallback),
      model: data?.model,
    })
  } catch (error) {
    console.error(error)
    const isTimeout = error?.code === 'ECONNABORTED'
    ElMessage.error(isTimeout ? '模型回复较慢，请稍后重试或换一个更短的问题' : 'Agent 请求失败，请确认后端服务已启动')
    messages.value.push({
      role: 'assistant',
      content: isTimeout
        ? '模型这次回复超过了等待时间。你可以把问题说得更短一些，或稍后再试。'
        : '我暂时连接不上后端 Agent 服务。请先确认 Spring Boot 后端正在运行，再重新发送问题。',
      fallback: true,
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const usePrompt = (prompt) => {
  input.value = prompt.text
  sendMessage(prompt.text)
}
</script>

<template>
  <section class="agent-page">
    <el-card class="module-card agent-card">
      <div class="module-header agent-header">
        <div>
          <p class="demo-eyebrow">Travel Agent</p>
          <h2>旅游服务 Agent</h2>
          <p class="module-subtitle">面向游客和校园参观者，支持推荐、路线解释、设施查询、美食、日记和攻略建议。</p>
        </div>
        <div class="agent-status">
          <Robot theme="outline" size="18" fill="currentColor" />
          SiliconFlow
        </div>
      </div>

      <div class="quick-prompts" aria-label="快捷问题">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt.label"
          class="quick-prompt"
          type="button"
          :disabled="loading"
          @click="usePrompt(prompt)"
        >
          <component :is="prompt.icon" theme="outline" size="16" fill="currentColor" />
          {{ prompt.label }}
        </button>
      </div>

      <div ref="messageListRef" class="chat-panel">
        <article
          v-for="(message, index) in messages"
          :key="`${message.role}-${index}`"
          class="chat-message"
          :class="message.role"
        >
          <div class="message-avatar">
            <Robot v-if="message.role === 'assistant'" theme="outline" size="18" fill="currentColor" />
            <span v-else>我</span>
          </div>
          <div class="message-bubble">
            <p>{{ message.content }}</p>
            <small v-if="message.fallback">本地安全回退回复</small>
            <small v-else-if="message.model">模型：{{ message.model }}</small>
          </div>
        </article>
        <article v-if="loading" class="chat-message assistant typing-message">
          <div class="message-avatar">
            <Robot theme="outline" size="18" fill="currentColor" />
          </div>
          <div class="message-bubble typing-bubble" aria-label="Agent 正在回复">
            <span class="typing-dot"></span>
          </div>
        </article>
      </div>

      <div class="agent-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="800"
          show-word-limit
          placeholder="例如：从校门到图书馆怎么规划路线？附近有什么吃饭的地方？"
          @keydown.enter.exact.prevent="sendMessage()"
        />
        <el-button type="primary" size="large" :disabled="!canSend" @click="sendMessage()">
          <Send theme="outline" size="16" fill="currentColor" />
          发送
        </el-button>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.agent-page {
  display: grid;
  gap: 18px;
}

.agent-card {
  min-height: 680px;
}

.agent-header {
  align-items: center;
}

.agent-status {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(255, 56, 92, 0.18);
  background: #fff1f3;
  color: var(--demo-coral-dark);
  padding: 0 12px;
  font-size: 13px;
  font-weight: 900;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
}

.quick-prompt {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border-radius: 999px;
  border: 1px solid var(--demo-border);
  background: #ffffff;
  color: var(--demo-ink);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.quick-prompt:hover,
.quick-prompt:focus-visible {
  border-color: rgba(255, 56, 92, 0.38);
  color: var(--demo-coral-dark);
}

.quick-prompt:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.chat-panel {
  height: 420px;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 16px;
  border-radius: 18px;
  border: 1px solid var(--demo-border);
  background: #f8fafc;
  padding: 18px;
}

.chat-message {
  display: flex;
  gap: 10px;
  max-width: min(760px, 92%);
}

.chat-message.user {
  justify-self: end;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
}

.chat-message.user .message-avatar {
  background: var(--demo-coral);
}

.message-bubble {
  border-radius: 18px;
  border: 1px solid var(--demo-border);
  background: #ffffff;
  color: var(--demo-ink);
  padding: 12px 14px;
  box-shadow: var(--demo-shadow-soft);
}

.chat-message.user .message-bubble {
  background: #111827;
  color: #f8fafc;
  border-color: rgba(17, 24, 39, 0.22);
}

.message-bubble p {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-size: 14px;
  font-weight: 500;
}

.message-bubble small {
  display: block;
  margin-top: 8px;
  color: var(--demo-muted);
  font-size: 12px;
  font-weight: 800;
}

.chat-message.user .message-bubble small {
  color: rgba(248, 250, 252, 0.66);
}

.typing-message {
  animation: typing-fade-in 160ms ease-out;
}

.typing-bubble {
  min-width: 48px;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--demo-coral);
  animation: typing-dot-pulse 900ms ease-in-out infinite;
}

@keyframes typing-dot-pulse {
  0%,
  100% {
    opacity: 0.28;
    transform: scale(0.78);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes typing-fade-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.agent-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
}

.agent-input :deep(.el-button) {
  min-width: 108px;
  gap: 6px;
}

@media (max-width: 744px) {
  .agent-card {
    min-height: 620px;
  }

  .agent-header {
    align-items: flex-start;
  }

  .agent-status {
    margin-top: 12px;
  }

  .chat-panel {
    height: 380px;
    padding: 14px;
  }

  .chat-message {
    max-width: 100%;
  }

  .agent-input {
    grid-template-columns: 1fr;
  }

  .agent-input :deep(.el-button) {
    width: 100%;
  }
}
</style>
