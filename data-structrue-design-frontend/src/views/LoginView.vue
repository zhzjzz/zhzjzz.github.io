<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/travel'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

import scene1 from '../assets/login-scene-1.svg'
import scene2 from '../assets/login-scene-2.svg'
import scene3 from '../assets/login-scene-3.svg'

/**
 * 登录页风景图数据：通过纵向滚动形成沉浸式长页面。
 */
const scenes = [
  { title: '极光穹顶', subtitle: '智能体引擎实时编排你的旅行节奏', image: scene1 },
  { title: '云岭深境', subtitle: '目的地推荐与兴趣画像持续联动', image: scene2 },
  { title: '海岸跃迁', subtitle: '路线规划自动融合交通与时间窗口', image: scene3 },
  { title: '星河步道', subtitle: '旅途中的打卡、导航与日记无缝衔接', image: scene1 },
  { title: '雾森回廊', subtitle: '多人协作行程，实时同步每一步决策', image: scene2 },
  { title: '暮色港湾', subtitle: '用更具科技感的方式记录每段风景', image: scene3 },
]

const authMode = ref('login')
const formRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const registerLoading = ref(false)
const scrollerRef = ref()
const sceneRefs = ref([])
const activeSceneIndex = ref(0)

const loginForm = ref({
  username: 'demo',
  password: '123456',
})

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!value || value !== registerForm.value.password) {
          callback(new Error('两次密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const router = useRouter()
const appStore = useAppStore()
const isLoginMode = computed(() => authMode.value === 'login')

/**
 * 根据风景卡片与视口中心点距离，动态计算“焦点风景”。
 */
const syncActiveScene = () => {
  if (!sceneRefs.value.length) return
  const viewportCenter = window.innerHeight / 2
  let nearestIndex = 0
  let nearestDistance = Number.POSITIVE_INFINITY

  sceneRefs.value.forEach((sceneEl, index) => {
    if (!sceneEl) return
    const { top, height } = sceneEl.getBoundingClientRect()
    const distance = Math.abs(top + height / 2 - viewportCenter)
    if (distance < nearestDistance) {
      nearestDistance = distance
      nearestIndex = index
    }
  })

  activeSceneIndex.value = nearestIndex
}

/**
 * 登录提交：当前仅展示界面效果，默认不做登录后重定向。
 */
const submitLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(loginForm.value)
    appStore.setLogin({ token: data.token, displayName: data.displayName })
    ElMessage.success(data.message || '登录成功（当前为界面演示模式）')
    const redirectAfterLogin = false
    if (redirectAfterLogin) {
      router.push('/')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

/**
 * 注册提交：当前未接入后端注册接口，仅用于界面演示。
 */
const submitRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate()
  registerLoading.value = true
  setTimeout(() => {
    registerLoading.value = false
    ElMessage.success('注册界面演示完成，可继续切换到登录')
    authMode.value = 'login'
  }, 400)
}

const setSceneRef = (el, index) => {
  if (el) {
    sceneRefs.value[index] = el
  }
}

const getSceneClass = (index) => {
  const offset = Math.abs(index - activeSceneIndex.value)
  if (offset === 0) return 'scene-card active'
  if (offset === 1) return 'scene-card near'
  return 'scene-card far'
}

const handleScroll = () => {
  syncActiveScene()
}

onMounted(async () => {
  await nextTick()
  syncActiveScene()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleScroll)
})
</script>

<template>
  <section class="login-page">
    <header class="top-bar">
      <div class="brand">
        <strong>Travel.AI</strong>
        <span>智能旅行中枢</span>
      </div>
      <div class="auth-switch">
        <button
          class="ghost-btn"
          :class="{ active: isLoginMode }"
          type="button"
          @click="authMode = 'login'"
        >
          登录
        </button>
        <button
          class="solid-btn"
          :class="{ active: !isLoginMode }"
          type="button"
          @click="authMode = 'register'"
        >
          注册
        </button>
      </div>
    </header>

    <div ref="scrollerRef" class="scene-flow">
      <article
        v-for="(scene, index) in scenes"
        :key="`${scene.title}-${index}`"
        :ref="(el) => setSceneRef(el, index)"
        :class="getSceneClass(index)"
      >
        <img :src="scene.image" :alt="scene.title" class="scene-image" />
        <div class="scene-mask"></div>
        <div class="scene-text">
          <h1>{{ scene.title }}</h1>
          <p>{{ scene.subtitle }}</p>
        </div>
      </article>
    </div>

    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-title">
          <h2>{{ isLoginMode ? '欢迎登录个性化旅游系统' : '创建你的旅行账号' }}</h2>
          <p>{{ isLoginMode ? '请输入账号与密码后进入系统' : '注册仅做前端展示，暂不跳转' }}</p>
        </div>
      </template>

      <el-form
        v-if="isLoginMode"
        ref="formRef"
        :model="loginForm"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submitLogin">
          立即登录
        </el-button>
      </el-form>

      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            size="large"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="registerLoading" @click="submitRegister">
          创建账号
        </el-button>
      </el-form>

      <div class="tips">
        <p>演示账号：demo / admin / guest</p>
        <p>登录后跳转代码已预留，可按需开启。</p>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  padding: 20px 0 80px;
  background:
    radial-gradient(circle at 20% 10%, rgba(59, 130, 246, 0.2), transparent 38%),
    radial-gradient(circle at 85% 25%, rgba(16, 185, 129, 0.2), transparent 40%),
    #020617;
}

.top-bar {
  position: fixed;
  left: 36px;
  right: 36px;
  top: 24px;
  z-index: 30;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.brand {
  display: flex;
  flex-direction: column;
  color: #dbeafe;
}

.brand strong {
  font-size: 20px;
  letter-spacing: 1px;
}

.brand span {
  font-size: 12px;
  color: rgba(219, 234, 254, 0.78);
}

.auth-switch {
  display: flex;
  gap: 10px;
}

.ghost-btn,
.solid-btn {
  min-width: 82px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  background: rgba(15, 23, 42, 0.45);
  color: #e2e8f0;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.28s ease;
}

.solid-btn {
  background: linear-gradient(120deg, #2563eb, #06b6d4);
  border: none;
}

.ghost-btn.active,
.solid-btn.active {
  box-shadow: 0 0 0 1px rgba(14, 165, 233, 0.5), 0 10px 28px rgba(14, 165, 233, 0.32);
  transform: translateY(-1px);
}

.scene-flow {
  position: relative;
  width: min(900px, calc(100vw - 520px));
  margin-left: 3vw;
  padding-top: 92px;
  display: flex;
  flex-direction: column;
  gap: 34px;
}

.scene-card {
  position: relative;
  min-height: 70vh;
  border-radius: 24px;
  overflow: hidden;
  transition: transform 0.45s ease, filter 0.45s ease, opacity 0.45s ease;
  transform: scale(0.88);
  filter: blur(3px) saturate(0.85);
  opacity: 0.58;
}

.scene-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.02);
}

.scene-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(140deg, rgba(2, 6, 23, 0.72), rgba(2, 6, 23, 0.18)),
    radial-gradient(circle at 80% 20%, rgba(14, 165, 233, 0.35), transparent 38%);
}

.scene-text {
  position: absolute;
  left: 5%;
  bottom: 10%;
  color: #f8fafc;
  max-width: 520px;
}

.scene-text h1 {
  font-size: clamp(28px, 4vw, 46px);
  margin: 0 0 14px;
  text-shadow: 0 10px 35px rgba(2, 6, 23, 0.8);
}

.scene-text p {
  font-size: clamp(14px, 2vw, 18px);
  opacity: 0.96;
}

.scene-card.active {
  transform: scale(1);
  filter: blur(0) saturate(1);
  opacity: 1;
}

.scene-card.active .scene-image {
  animation: breathing 7s ease-in-out infinite;
}

.scene-card.near {
  transform: scale(0.94);
  filter: blur(1.2px) saturate(0.95);
  opacity: 0.82;
}

.login-card {
  position: fixed;
  right: 6vw;
  top: 54%;
  transform: translateY(-50%);
  width: min(420px, 88vw);
  border: 1px solid rgba(148, 163, 184, 0.28);
  backdrop-filter: blur(14px);
  background: rgba(255, 255, 255, 0.9);
  z-index: 20;
  box-shadow: 0 24px 42px rgba(2, 6, 23, 0.35);
}

.login-title h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.login-title p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}

.tips {
  margin-top: 12px;
  color: #475569;
  font-size: 12px;
  line-height: 1.7;
}

@keyframes breathing {
  0%, 100% { transform: scale(1.02); }
  50% { transform: scale(1.07); }
}

@media (max-width: 900px) {
  .top-bar {
    left: 16px;
    right: 16px;
    top: 14px;
  }

  .scene-flow {
    width: 94vw;
    margin: 54px auto 0;
    padding-top: 24px;
  }

  .login-card {
    position: static;
    margin: 22px auto 0;
    transform: none;
  }
}
</style>
