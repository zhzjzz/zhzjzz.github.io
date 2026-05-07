<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/travel'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import scene1 from '../assets/login-scene-1.svg'

const authMode = ref('login')
const formRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const registerLoading = ref(false)

const loginForm = ref({
  username: 'demo',
  password: '123456',
})

const registerForm = ref({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
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

const submitLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(loginForm.value)
    appStore.setLogin({ token: data.token, displayName: data.displayName, interests: ['校园', '博物馆', '小吃'] })
    ElMessage.success(data.message || '登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

const submitRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    const { data } = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      displayName: registerForm.value.displayName,
    })
    appStore.setLogin({ token: data.token, displayName: data.displayName, interests: ['校园', '博物馆', '小吃'] })
    registerForm.value = {
      username: '',
      displayName: '',
      password: '',
      confirmPassword: '',
    }
    ElMessage.success(data.message || '注册成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <section class="login-page">
    <article class="hero-panel">
      <div class="hero-copy">
        <span>Travel.AI</span>
        <h1>Belong anywhere</h1>
        <p>登录后即可使用推荐、路线、日记与协作行程能力。</p>
      </div>
      <img :src="scene1" alt="travel scene" class="hero-image" />
    </article>

    <el-card class="login-card">
      <div class="auth-switch" role="tablist" aria-label="登录注册切换">
        <button
          class="auth-btn"
          :class="{ active: isLoginMode }"
          type="button"
          role="tab"
          :aria-selected="isLoginMode"
          @click="authMode = 'login'"
        >
          登录
        </button>
        <button
          class="auth-btn"
          :class="{ active: !isLoginMode }"
          type="button"
          role="tab"
          :aria-selected="!isLoginMode"
          @click="authMode = 'register'"
        >
          注册
        </button>
      </div>

      <div class="login-title">
        <h2>{{ isLoginMode ? '欢迎回来' : '创建账号' }}</h2>
        <p>{{ isLoginMode ? '请输入账号与密码' : '注册仅用于前端演示' }}</p>
      </div>

      <el-form v-if="isLoginMode" ref="formRef" :model="loginForm" :rules="formRules" label-position="top">
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

      <el-form v-else ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" clearable />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="registerForm.displayName" placeholder="例如：张三 / Alice" size="large" clearable />
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
        <p>默认账号：demo / 123456，admin / admin123，guest / guest123</p>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 32px;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
  background: #ffffff;
}

.hero-panel {
  border-radius: 32px;
  overflow: hidden;
  background: #f7f7f7;
  box-shadow: rgba(0, 0, 0, 0.02) 0px 0px 0px 1px,
    rgba(0, 0, 0, 0.04) 0px 2px 6px,
    rgba(0, 0, 0, 0.1) 0px 4px 8px;
  position: relative;
  min-height: 720px;
}

.hero-copy {
  position: absolute;
  left: 32px;
  top: 32px;
  z-index: 2;
  color: #222222;
}

.hero-copy span {
  color: #ff385c;
  font-weight: 700;
}

.hero-copy h1 {
  margin-top: 8px;
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.1;
  letter-spacing: -0.44px;
}

.hero-copy p {
  margin-top: 10px;
  color: #6a6a6a;
  font-size: 14px;
}

.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-card {
  align-self: center;
  border-radius: 20px;
  border: none;
  box-shadow: rgba(0, 0, 0, 0.02) 0px 0px 0px 1px,
    rgba(0, 0, 0, 0.04) 0px 2px 6px,
    rgba(0, 0, 0, 0.1) 0px 4px 8px;
}

.auth-switch {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.auth-btn {
  border: none;
  border-radius: 8px;
  padding: 10px 12px;
  background: #f2f2f2;
  color: #6a6a6a;
  font-weight: 600;
  cursor: pointer;
}

.auth-btn.active {
  background: #ff385c;
  color: #ffffff;
}

.login-title {
  margin: 20px 0;
}

.login-title h2 {
  color: #222222;
  font-size: 24px;
}

.login-title p {
  margin-top: 6px;
  color: #6a6a6a;
  font-size: 14px;
}

.login-btn {
  width: 100%;
}

.tips {
  margin-top: 12px;
  color: #6a6a6a;
  font-size: 12px;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .hero-panel {
    min-height: 320px;
  }
}
</style>
