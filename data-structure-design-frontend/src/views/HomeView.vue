<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTopFoods } from '../api/travel'

const foods = ref([])
const router = useRouter()

const featureCards = [
  { title: '目的地推荐', text: '按评分、热度和综合分生成 Top10 推荐。', path: '/destinations', accent: 'coral' },
  { title: '多景区路线', text: '串联多个景区和地点，展示总距离、总耗时与分段路径。', path: '/routes', accent: 'teal' },
  { title: '附近场所', text: '从目的地坐标出发，查询周边服务设施。', path: '/facilities', accent: 'amber' },
  { title: '协作行程', text: '管理多人行程、策略、交通方式与备注。', path: '/itineraries', accent: 'blue' },
]

const dashboardStats = [
  { label: '推荐模块', value: '4' },
  { label: '演示路径', value: '1' },
  { label: '路线类型', value: '多景区' },
  { label: '数据源', value: 'SQLite' },
]

const loadFoods = async () => {
  const { data } = await getTopFoods(5)
  foods.value = data
}

const go = (path) => {
  router.push(path)
}

onMounted(loadFoods)
</script>

<template>
  <section class="home-page">
    <section class="demo-hero home-hero reveal-in">
      <div>
        <p class="demo-eyebrow">Travel AI Showcase</p>
        <h1 class="demo-title">个性化旅行推荐与路线规划系统</h1>
        <p class="demo-copy">
          面向课堂演示的完整旅行系统：从目的地推荐、场所查询到多景区路线规划和协作行程管理，形成一条清晰的功能闭环。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="go('/routes')">进入路线演示</el-button>
        <el-button size="large" @click="go('/destinations')">查看推荐榜单</el-button>
      </div>
    </section>

    <section class="demo-grid">
      <article v-for="item in dashboardStats" :key="item.label" class="metric-card reveal-in">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="feature-grid">
      <button
        v-for="item in featureCards"
        :key="item.title"
        type="button"
        class="feature-card reveal-in"
        :class="`accent-${item.accent}`"
        @click="go(item.path)"
      >
        <span>{{ item.title }}</span>
        <strong>{{ item.text }}</strong>
      </button>
    </section>

    <section class="food-strip">
      <div class="module-header">
        <div>
          <h2>美食推荐样例</h2>
          <p class="module-subtitle">用于展示系统可以扩展到餐饮、景点、路线等多类旅行数据。</p>
        </div>
      </div>
      <div class="listings">
        <article class="listing-card reveal-in" v-for="item in foods" :key="item.id">
          <div class="listing-media">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
            <div v-else class="image-placeholder">暂无图片</div>
          </div>
          <div class="listing-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.cuisine || '地方特色' }} · 评分 {{ item.rating || '-' }}</p>
            <strong>精选推荐</strong>
          </div>
        </article>
        <el-empty v-if="!foods.length" description="暂无推荐数据" />
      </div>
    </section>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.home-hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.feature-card {
  min-height: 150px;
  padding: 20px;
  text-align: left;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.14);
}

.feature-card span {
  color: #ff385c;
  font-size: 13px;
  font-weight: 900;
}

.feature-card strong {
  display: block;
  margin-top: 12px;
  color: #111827;
  font-size: 18px;
  line-height: 1.5;
}

.accent-teal span {
  color: #0f766e;
}

.accent-amber span {
  color: #d97706;
}

.accent-blue span {
  color: #2563eb;
}

.food-strip {
  display: grid;
  gap: 16px;
}

.listings {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.listing-card {
  border-radius: 20px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: rgba(0, 0, 0, 0.02) 0px 0px 0px 1px,
    rgba(0, 0, 0, 0.04) 0px 2px 6px,
    rgba(0, 0, 0, 0.1) 0px 4px 8px;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.listing-card:hover {
  transform: translateY(-3px);
  box-shadow: rgba(0, 0, 0, 0.04) 0 0 0 1px, rgba(0, 0, 0, 0.12) 0 14px 32px;
}

.listing-media {
  position: relative;
  aspect-ratio: 16 / 10;
}

.listing-media::after {
  content: '精选';
  position: absolute;
  right: 10px;
  top: 10px;
  height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: rgba(0, 0, 0, 0.08) 0px 4px 12px;
  color: #222222;
  font-size: 12px;
  font-weight: 800;
}

.listing-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f7f7f7, #efefef);
  color: #6a6a6a;
  font-size: 13px;
}

.listing-body {
  padding: 12px;
}

.listing-body h3 {
  font-size: 16px;
  font-weight: 600;
  color: #222222;
}

.listing-body p {
  margin-top: 4px;
  color: #6a6a6a;
  font-size: 13px;
}

.listing-body strong {
  display: inline-block;
  margin-top: 8px;
  color: #ff385c;
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 744px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
