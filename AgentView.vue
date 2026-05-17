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
    <section class="agent-console reveal-in">
      <aside class="agent-orbit">
        <div class="agent-core">
          <span class="core-ring"></span>
          <Robot theme="outline" size="34" fill="currentColor" />
        </div>
        <div class="agent-title">
          <p class="demo-eyebrow">Travel Agent</p>
          <h2>旅游服务 Agent</h2>
          <p>面向游客和校园参观者，处理推荐、路线、设施、美食、日记和攻略问题。</p>
        </div>
        <div class="agent-status">
          <Robot theme="outline" size="18" fill="currentColor" />
          <span>SiliconFlow 在线</span>
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
            <component :is="prompt.icon" theme="outline" size="17" fill="currentColor" />
            <span>{{ prompt.label }}</span>
          </button>
        </div>
      </aside>

      <section class="chat-workspace">
        <header class="chat-header">
          <div>
            <p class="demo-eyebrow">Live Console</p>
            <h3>智能旅行对话</h3>
          </div>
          <span class="signal-pill" :class="{ active: loading }">
            <span></span>
            {{ loading ? '生成中' : '待命' }}
          </span>
        </header>

        <div ref="messageListRef" class="chat-panel">
          <article
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            class="chat-message"
            :class="message.role"
            :style="{ '--message-index': index }"
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
              <span class="typing-dot"></span>
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
      </section>
    </section>
  </section>
</template>

<style scoped>
.agent-page {
  --agent-accent: #ff385c;
  --agent-warm: #f3d08a;
  --agent-cyan: #38bdf8;
  --agent-panel: rgba(23, 25, 29, 0.88);
  --agent-border: rgba(255, 255, 255, 0.1);
  --el-color-primary: #ff385c;
  --el-fill-color-blank: #17191d;
  --el-text-color-primary: #f8fafc;
  --el-text-color-regular: #a7b0bf;
  --el-border-color: rgba(255, 255, 255, 0.12);
  min-height: calc(100vh - 140px);
  color: #f8fafc;
  background:
    linear-gradient(135deg, rgba(255, 56, 92, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(23, 25, 29, 0.96), #0d0f12 58%);
}

.agent-console {
  position: relative;
  display: grid;
  grid-template-columns: minmax(280px, 0.38fr) minmax(0, 1fr);
  gap: 18px;
  min-height: 690px;
}

.agent-console::before {
  content: '';
  position: absolute;
  inset: 34px 10% auto 16%;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(255, 56, 92, 0.7), rgba(56, 189, 248, 0.55), transparent);
  animation: consoleBeam 3.4s var(--motion-ease) infinite;
}

.agent-orbit,
.chat-workspace {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--agent-border);
  border-radius: 18px;
  background: var(--agent-panel);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.36);
  backdrop-filter: blur(18px);
}

.agent-orbit::before,
.chat-workspace::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(120deg, transparent 8%, rgba(255, 255, 255, 0.14) 18%, transparent 30%);
  opacity: 0;
  transform: translateX(-80%);
}

.agent-orbit:hover::before,
.chat-workspace:hover::before {
  animation: agentSheen 980ms var(--motion-ease);
}

.agent-orbit {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 24px;
}

.agent-core {
  position: relative;
  width: 112px;
  height: 112px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background:
    radial-gradient(circle at 38% 32%, rgba(255, 255, 255, 0.34), transparent 26%),
    linear-gradient(135deg, var(--agent-accent), #1d4ed8);
  box-shadow: 0 24px 60px rgba(255, 56, 92, 0.28), 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.core-ring,
.agent-core::after {
  position: absolute;
  content: '';
  inset: -10px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.core-ring {
  animation: orbitSpin 8s linear infinite;
}

.core-ring::before {
  content: '';
  position: absolute;
  top: 12px;
  left: 50%;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--agent-warm);
  box-shadow: 0 0 22px rgba(243, 208, 138, 0.82);
}

.agent-core::after {
  inset: -20px;
  opacity: 0.42;
  animation: pulseRing 2.6s ease-in-out infinite;
}

.agent-title h2 {
  margin-top: 8px;
  color: #f8fafc;
  font-size: 32px;
  line-height: 1.12;
  font-weight: 900;
  letter-spacing: 0;
}

.agent-title p:last-child {
  margin-top: 12px;
  color: #a7b0bf;
  line-height: 1.75;
  font-size: 14px;
}

.agent-status {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.28);
  background: rgba(56, 189, 248, 0.1);
  color: #bae6fd;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 900;
  justify-self: start;
}

.quick-prompts {
  display: grid;
  gap: 10px;
}

.quick-prompt {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.055);
  color: #f8fafc;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  text-align: left;
  transition: transform 220ms var(--motion-spring), border-color 220ms ease, background 220ms ease, box-shadow 220ms ease;
}

.quick-prompt:hover,
.quick-prompt:focus-visible {
  transform: translateX(6px);
  border-color: rgba(255, 56, 92, 0.42);
  background: rgba(255, 56, 92, 0.12);
  box-shadow: 0 18px 42px rgba(255, 56, 92, 0.12);
}

.quick-prompt:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.chat-workspace {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 20px;
}

.chat-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 16px;
}

.chat-header h3 {
  margin-top: 6px;
  color: #f8fafc;
  font-size: 26px;
  line-height: 1.18;
  font-weight: 900;
  letter-spacing: 0;
}

.signal-pill {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 0 12px;
  color: #a7b0bf;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  font-weight: 900;
}

.signal-pill span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 18px rgba(34, 197, 94, 0.74);
}

.signal-pill.active span {
  background: var(--agent-accent);
  animation: statusPulse 700ms ease-in-out infinite;
}

.chat-panel {
  min-height: 0;
  overflow-y: auto;
  display: grid;
  align-content: start;
  gap: 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.035) 0 1px, transparent 1px 32px);
  padding: 18px;
  scrollbar-color: rgba(255, 56, 92, 0.42) transparent;
}

.chat-message {
  display: flex;
  gap: 10px;
  max-width: min(760px, 92%);
  animation: messageRise 360ms var(--motion-spring) both;
  animation-delay: calc(min(var(--message-index), 8) * 24ms);
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
  border-radius: 12px;
  background: linear-gradient(135deg, #24272d, #111214);
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.24);
}

.chat-message.user .message-avatar {
  background: linear-gradient(135deg, var(--agent-accent), #f3d08a);
}

.message-bubble {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.075);
  color: #f8fafc;
  padding: 12px 14px;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.22);
}

.chat-message.user .message-bubble {
  background: linear-gradient(135deg, rgba(255, 56, 92, 0.88), rgba(217, 15, 63, 0.8));
  color: #f8fafc;
  border-color: rgba(255, 56, 92, 0.3);
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
  color: #a7b0bf;
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
  min-width: 64px;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: none;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--agent-accent);
  animation: typing-dot-pulse 900ms ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 120ms;
}

.typing-dot:nth-child(3) {
  animation-delay: 240ms;
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
  padding-top: 16px;
}

.agent-input :deep(.el-button) {
  min-width: 108px;
  gap: 6px;
  min-height: 52px;
  box-shadow: 0 18px 42px rgba(255, 56, 92, 0.2);
}

.agent-input :deep(.el-textarea__inner) {
  min-height: 86px !important;
  border-radius: 14px;
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: #f8fafc;
  box-shadow: none;
  transition: border-color 220ms ease, box-shadow 220ms ease, background 220ms ease;
}

.agent-input :deep(.el-textarea__inner:focus) {
  border-color: rgba(255, 56, 92, 0.56);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(255, 56, 92, 0.13);
}

.agent-input :deep(.el-input__count) {
  color: #a7b0bf;
  background: transparent;
}

@media (max-width: 744px) {
  .agent-console {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .agent-orbit {
    padding: 20px;
  }

  .agent-core {
    width: 86px;
    height: 86px;
  }

  .chat-panel {
    height: 390px;
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

@keyframes agentSheen {
  0% {
    opacity: 0;
    transform: translateX(-80%);
  }
  24%,
  62% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(80%);
  }
}

@keyframes consoleBeam {
  0%,
  100% {
    opacity: 0.18;
    transform: translateX(-12%);
  }
  50% {
    opacity: 0.72;
    transform: translateX(12%);
  }
}

@keyframes orbitSpin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulseRing {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(0.96);
  }
  50% {
    opacity: 0.56;
    transform: scale(1.04);
  }
}

@keyframes statusPulse {
  0%,
  100% {
    opacity: 0.42;
    transform: scale(0.82);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

@keyframes messageRise {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.985);
    filter: blur(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}
</style>
