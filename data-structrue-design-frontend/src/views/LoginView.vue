<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/travel'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

import scene1 from '../assets/login-scene-1.svg'
import scene2 from '../assets/login-scene-2.svg'
import scene3 from '../assets/login-scene-3.svg'

/**
 * 登录页风景图数据：通过上下滑动切换，形成沉浸式引导效果。
 */
const scenes = [
  { title: '山水漫游', subtitle: '景区与校园双场景，开启智能探索', image: scene1 },
  { title: '林间拾趣', subtitle: '实时推荐附近打卡点与特色美食', image: scene2 },
  { title: '暮光行记', subtitle: '路线规划、日记管理、协作行程一站式完成', image: scene3 },
]

const currentIndex = ref(0)
const formRef = ref()
const loading = ref(false)
const loginForm = ref({
  username: 'demo',
  password: '123456',
})
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const router = useRouter()
const appStore = useAppStore()
const currentScene = computed(() => scenes[currentIndex.value])

/**
 * 上下滚轮切换风景图，制造动态滑动观感。
 */
const onWheel = (event) => {
  if (Math.abs(event.deltaY) < 8) return
  if (event.deltaY > 0) {
    currentIndex.value = (currentIndex.value + 1) % scenes.length
  } else {
    currentIndex.value = (currentIndex.value - 1 + scenes.length) % scenes.length
  }
}

/**
 * 提交登录：校验表单后调用后端登录接口，并保存登录状态。
 */
const submitLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(loginForm.value)
    appStore.setLogin({ token: data.token, displayName: data.displayName })
    ElMessage.success(data.message || '登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-page" @wheel.prevent="onWheel">
    <transition name="scene-fade" mode="out-in">
      <div class="scene-layer" :key="currentScene.image">
        <img :src="currentScene.image" alt="风景背景图" class="scene-image" />
        <div class="scene-mask"></div>
        <div class="scene-text">
          <h1>{{ currentScene.title }}</h1>
          <p>{{ currentScene.subtitle }}</p>
        </div>
      </div>
    </transition>

    <el-card class="login-card" shadow="hover">
      <template #header>
        <div class="login-title">
          <h2>欢迎登录个性化旅游系统</h2>
          <p>请输入账号与密码后进入系统</p>
        </div>
      </template>
      <el-form ref="formRef" :model="loginForm" :rules="formRules" label-position="top">
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
      <div class="tips">
        <p>演示账号：demo / admin / guest</p>
        <p>密码信息请使用课程组分发的测试账号配置。</p>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.scene-layer {
  position: absolute;
  inset: 0;
}

.scene-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.05);
  animation: floatScene 8s ease-in-out infinite;
}

.scene-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(15, 23, 42, 0.7), rgba(15, 23, 42, 0.25));
}

.scene-text {
  position: absolute;
  left: 6vw;
  bottom: 16vh;
  color: #f8fafc;
  max-width: 460px;
}

.scene-text h1 {
  font-size: 42px;
  margin: 0 0 12px;
}

.scene-text p {
  font-size: 18px;
  opacity: 0.95;
}

.login-card {
  position: absolute;
  right: 6vw;
  top: 50%;
  transform: translateY(-50%);
  width: min(420px, 88vw);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.9);
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

.scene-fade-enter-active,
.scene-fade-leave-active {
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.scene-fade-enter-from,
.scene-fade-leave-to {
  opacity: 0;
  transform: translateY(24px);
}

@keyframes floatScene {
  0%, 100% { transform: scale(1.05) translateY(0); }
  50% { transform: scale(1.08) translateY(-8px); }
}

@media (max-width: 900px) {
  .scene-text {
    left: 4vw;
    right: 4vw;
    bottom: auto;
    top: 7vh;
  }

  .scene-text h1 {
    font-size: 30px;
  }

  .scene-text p {
    font-size: 15px;
  }

  .login-card {
    left: 50%;
    right: auto;
    top: auto;
    bottom: 6vh;
    transform: translateX(-50%);
  }
}
</style>
