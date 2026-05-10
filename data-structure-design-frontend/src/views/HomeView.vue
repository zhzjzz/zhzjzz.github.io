<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, MapDraw, MapRoad, Search, Shop } from '@icon-park/vue-next'
import { getTopFoods } from '../api/travel'
import heroImage from '../assets/workshop-extracted/workshop-2827305814-1.jpg'
import foodDefaultImage from '../assets/defaults/food-default.png'

const router = useRouter()
const route = useRoute()
const foods = ref([])
const foodError = ref('')

const heroMetrics = [
  { value: '24h', label: '短途灵感' },
  { value: 'Top', label: '精选目的地' },
  { value: 'Plan', label: '路线组合' },
]

const experienceCards = [
  {
    icon: MapDraw,
    label: 'DESTINATION',
    title: '先找到值得出发的地方',
    text: '综合推荐 · 热度排序 · 关键词检索',
    path: '/destinations',
  },
  {
    icon: MapRoad,
    label: 'ROUTE',
    title: '把多个景点串成一条顺路行程',
    text: '多景区串联 · 顺路规划 · 交通方式',
    path: '/routes',
  },
  {
    icon: Shop,
    label: 'AROUND',
    title: '把附近服务也放进行程里',
    text: '附近服务 · 餐饮补给 · 周边设施',
    path: '/facilities',
  },
]

const editorialCards = [
  { title: '城市漫游', text: '适合一天内轻松走完的景区和周边组合。', path: '/destinations' },
  { title: '美食顺路吃', text: '把餐饮推荐和路线节点放在同一条体验线上。', path: '/facilities' },
  { title: '多人小队出发', text: '行程、备注和安排集中管理，让同行的人更容易对齐。', path: '/itineraries' },
]

const loadFoods = async () => {
  try {
    const { data } = await getTopFoods(6)
    foods.value = Array.isArray(data) ? data : []
    foodError.value = ''
  } catch (error) {
    foods.value = []
    foodError.value = '美食推荐暂时不可用'
  }
}

const go = (path) => {
  router.push(path)
}

const foodDiaryKeyword = (food) => {
  const name = food?.name || ''
  return name.includes('炸酱面') ? '北京炸酱面' : ''
}

const goFoodDiary = (food) => {
  const keyword = foodDiaryKeyword(food)
  if (!keyword) return
  router.push({ path: '/diaries', query: { keyword } })
}

const scrollToFoodSection = async () => {
  if (route.query.section !== 'foods') return
  await nextTick()
  document.querySelector('#food-recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  await loadFoods()
  await scrollToFoodSection()
})

watch(() => route.query.section, scrollToFoodSection)
</script>

<template>
  <section class="consumer-home">
    <section class="hero-scene reveal-in">
      <img class="hero-object" :src="heroImage" alt="" aria-hidden="true" />
      <img class="hero-motion-layer" :src="heroImage" alt="" aria-hidden="true" />
      <div class="hero-content">
        <p class="hero-kicker">Travel.AI</p>
        <h1>把下一次出发，变成一眼心动的旅行计划</h1>
        <div class="hero-actions" aria-label="首页主要操作">
          <el-button type="primary" size="large" @click="go('/destinations')">
            <Search theme="outline" size="18" fill="currentColor" />
            开始探索
          </el-button>
          <el-button size="large" class="ghost-button" @click="go('/routes')">
            规划路线
            <ArrowRight theme="outline" size="18" fill="currentColor" />
          </el-button>
        </div>
      </div>
      <div class="hero-metrics" aria-label="旅行灵感摘要">
        <article v-for="item in heroMetrics" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </article>
      </div>
    </section>

    <section class="search-entry reveal-in" aria-label="快速探索">
      <button type="button" class="search-entry-main" @click="go('/destinations')">
        <Search theme="outline" size="18" fill="currentColor" />
        <span>想去哪儿看看？</span>
      </button>
      <button type="button" @click="go('/routes')">路线灵感</button>
      <button type="button" @click="go('/facilities')">附近服务</button>
    </section>

    <section class="experience-grid" aria-label="旅行体验入口">
      <button
        v-for="item in experienceCards"
        :key="item.title"
        type="button"
        class="experience-card reveal-in"
        @click="go(item.path)"
      >
        <span class="experience-icon">
          <component :is="item.icon" theme="outline" size="24" fill="currentColor" />
        </span>
        <span class="experience-label">{{ item.label }}</span>
        <strong>{{ item.title }}</strong>
        <span class="experience-text">{{ item.text }}</span>
        <span class="experience-link">
          查看
          <ArrowRight theme="outline" size="16" fill="currentColor" />
        </span>
      </button>
    </section>

    <section class="editorial-section">
      <div class="section-heading">
        <p>精选灵感</p>
        <h2>挑一个心动场景，马上出发</h2>
      </div>
      <div class="editorial-grid">
        <button
          v-for="item in editorialCards"
          :key="item.title"
          type="button"
          class="editorial-card reveal-in"
          @click="go(item.path)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.text }}</span>
        </button>
      </div>
    </section>

    <section id="food-recommendations" class="food-showcase">
      <div class="section-heading">
        <p>Local Taste</p>
        <h2>顺手看看旅途中的好味道</h2>
      </div>

      <div v-if="foods.length" class="food-grid">
        <article
          v-for="item in foods"
          :key="item.id || item.name"
          class="food-card reveal-in"
          :class="{ 'food-card--link': foodDiaryKeyword(item) }"
          :role="foodDiaryKeyword(item) ? 'button' : undefined"
          :tabindex="foodDiaryKeyword(item) ? 0 : undefined"
          @click="goFoodDiary(item)"
          @keydown.enter="goFoodDiary(item)"
          @keydown.space.prevent="goFoodDiary(item)"
        >
          <div class="food-media">
            <img :src="item.imageUrl || foodDefaultImage" :alt="item.name || '美食推荐默认图'" loading="lazy" />
          </div>
          <div class="food-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.cuisine || '地方风味' }}</p>
            <span>评分 {{ item.rating || '-' }}</span>
          </div>
        </article>
      </div>

      <div v-else class="food-empty">
        <el-empty :description="foodError || '暂无推荐数据'" />
      </div>
    </section>
  </section>
</template>

<style scoped>
.consumer-home {
  display: grid;
  gap: 24px;
  color: #f8fafc;
}

.hero-scene {
  position: relative;
  min-height: 620px;
  overflow: hidden;
  border-radius: 20px;
  background: #111214;
  color: #ffffff;
  isolation: isolate;
  box-shadow: 0 34px 110px rgba(0, 0, 0, 0.38);
}

.hero-scene::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: radial-gradient(circle at 78% 26%, rgba(255, 255, 255, 0.16), transparent 24%);
  opacity: 0.46;
}

.hero-object,
.hero-motion-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}

.hero-object {
  z-index: 0;
  object-position: center 48%;
  opacity: 1;
  transform: scale(1.035);
  transform-origin: center;
  animation: heroCityDrift 28s ease-in-out infinite alternate;
}

.hero-motion-layer {
  z-index: 1;
  object-position: center 48%;
  opacity: 0.18;
  filter: saturate(1.24) contrast(1.06) blur(1px);
  mix-blend-mode: screen;
  transform: scale(1.11) translate3d(-1.4%, -0.6%, 0);
  animation: heroLightDrift 16s ease-in-out infinite alternate;
}

.hero-content,
.hero-metrics {
  z-index: 3;
}

.hero-content {
  position: absolute;
  left: 40px;
  right: auto;
  bottom: 28px;
  width: min(700px, calc(100% - 700px));
  min-width: 560px;
  padding: 22px 26px 24px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(7, 10, 16, 0.66), rgba(7, 10, 16, 0.34));
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(14px);
}

.hero-kicker,
.section-heading p,
.experience-label {
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-kicker {
  color: #f3d08a;
  font-size: 13px;
  font-weight: 800;
}

.hero-content h1 {
  margin-top: 10px;
  max-width: 640px;
  font-size: 42px;
  line-height: 1.08;
  font-weight: 900;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.48);
}

.hero-copy {
  margin-top: 18px;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 18px;
  line-height: 1.75;
  font-weight: 400;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.hero-actions .el-button {
  min-height: 48px;
  padding: 0 20px;
}

.ghost-button {
  border-color: rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.hero-metrics {
  position: absolute;
  left: auto;
  right: 40px;
  bottom: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 560px;
}

.hero-metrics article {
  min-height: 86px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  background: rgba(8, 10, 14, 0.44);
  backdrop-filter: blur(18px);
  animation: metricPulse 4.2s ease-in-out infinite;
}

.hero-metrics article:nth-child(2) {
  animation-delay: 320ms;
}

.hero-metrics article:nth-child(3) {
  animation-delay: 640ms;
}

@keyframes metricPulse {
  0%,
  100% {
    border-color: rgba(255, 255, 255, 0.16);
    box-shadow: none;
  }
  50% {
    border-color: rgba(243, 208, 138, 0.42);
    box-shadow: 0 0 30px rgba(243, 208, 138, 0.08);
  }
}

@keyframes heroCityDrift {
  from {
    transform: scale(1.035) translate3d(0, 0, 0);
  }
  to {
    transform: scale(1.07) translate3d(-1.2%, -0.8%, 0);
  }
}

@keyframes heroLightDrift {
  from {
    opacity: 0.14;
    transform: scale(1.11) translate3d(-1.4%, -0.6%, 0);
  }
  to {
    opacity: 0.25;
    transform: scale(1.16) translate3d(1.3%, 0.8%, 0);
  }
}

.hero-metrics strong,
.hero-metrics span {
  display: block;
}

.hero-metrics strong {
  font-size: 24px;
  line-height: 1;
  font-weight: 900;
}

.hero-metrics span {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.search-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(18px);
}

.search-entry button {
  min-height: 46px;
  border: 0;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 180ms ease, transform 180ms ease;
}

.search-entry button:hover,
.search-entry button:focus-visible {
  background: rgba(255, 255, 255, 0.14);
}

.search-entry-main {
  flex: 1;
  justify-content: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #f8fafc !important;
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.experience-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.experience-card,
.editorial-card,
.food-card {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: #17191d;
  box-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

.experience-card::before,
.editorial-card::before,
.food-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  border: 1px solid transparent;
  background: linear-gradient(135deg, rgba(255, 56, 92, 0.6), rgba(243, 208, 138, 0.32), transparent 42%) border-box;
  opacity: 0;
  mask: linear-gradient(#000 0 0) padding-box, linear-gradient(#000 0 0);
  mask-composite: exclude;
  transition: opacity 260ms ease;
}

.experience-card {
  min-height: 260px;
  padding: 22px;
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.experience-card:hover,
.experience-card:focus-visible,
.editorial-card:hover,
.editorial-card:focus-visible {
  border-color: rgba(255, 56, 92, 0.28);
  transform: translateY(-7px) rotateX(1deg);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.34);
}

.experience-card:hover::before,
.experience-card:focus-visible::before,
.editorial-card:hover::before,
.editorial-card:focus-visible::before,
.food-card:hover::before,
.food-card:focus-visible::before {
  opacity: 1;
}

.experience-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #f8fafc;
  color: #111214;
  font-size: 22px;
  transition: transform 260ms var(--motion-spring), box-shadow 260ms ease;
}

.experience-card:hover .experience-icon,
.experience-card:focus-visible .experience-icon {
  transform: translateY(-3px) rotate(-6deg) scale(1.06);
  box-shadow: 0 16px 34px rgba(255, 255, 255, 0.16);
}

.experience-link .i-icon {
  transition: transform 220ms var(--motion-spring);
}

.experience-card:hover .experience-link .i-icon,
.editorial-card:hover .experience-link .i-icon {
  transform: translateX(4px);
}

.experience-label {
  display: block;
  margin-top: 28px;
  color: #8a5d1d;
  font-size: 12px;
  font-weight: 900;
}

.experience-card strong {
  display: block;
  margin-top: 9px;
  color: #f8fafc;
  font-size: 22px;
  line-height: 1.28;
  font-weight: 900;
}

.experience-text {
  display: block;
  margin-top: 12px;
  color: #a7b0bf;
  font-size: 14px;
  line-height: 1.75;
  font-weight: 400;
}

.experience-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 22px;
  color: #ff385c;
  font-size: 14px;
  font-weight: 900;
}

.editorial-section,
.food-showcase {
  display: grid;
  gap: 18px;
  padding-top: 8px;
}

.section-heading p {
  color: #8a5d1d;
  font-size: 12px;
  font-weight: 900;
}

.section-heading h2 {
  margin-top: 6px;
  color: #f8fafc;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 900;
}

.editorial-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.editorial-card {
  min-height: 156px;
  padding: 22px;
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.editorial-card strong,
.editorial-card span {
  display: block;
}

.editorial-card strong {
  color: #f8fafc;
  font-size: 21px;
  line-height: 1.25;
  font-weight: 900;
}

.editorial-card span {
  margin-top: 12px;
  color: #a7b0bf;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 400;
}

.food-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.food-card {
  overflow: hidden;
}

.food-card--link {
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.food-card--link:hover,
.food-card--link:focus-visible {
  border-color: rgba(255, 56, 92, 0.28);
  transform: translateY(-7px);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.34);
}

.food-media {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #24272d;
}

.food-media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 620ms var(--motion-ease), filter 620ms var(--motion-ease);
}

.food-card:hover .food-media img {
  transform: scale(1.06);
  filter: saturate(1.12) contrast(1.04);
}

.food-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #f3d08a;
  font-size: 28px;
  background: #23201a;
}

.food-body {
  padding: 16px;
}

.food-body h3 {
  color: #f8fafc;
  font-size: 18px;
  line-height: 1.25;
  font-weight: 900;
}

.food-body p {
  margin-top: 6px;
  color: #a7b0bf;
  font-size: 14px;
  font-weight: 400;
}

.food-body span {
  display: inline-flex;
  margin-top: 12px;
  color: #ff385c;
  font-size: 13px;
  font-weight: 900;
}

.food-empty {
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 14px;
  background: #17191d;
}

button:focus-visible {
  outline: 3px solid rgba(255, 56, 92, 0.22);
  outline-offset: 3px;
}

@media (max-width: 1128px) {
  .hero-content {
    left: 28px;
    right: 28px;
    bottom: 144px;
    width: auto;
    min-width: 0;
  }

  .hero-content h1 {
    font-size: 42px;
  }

  .hero-object,
  .hero-motion-layer {
    object-position: 58% center;
  }

  .experience-grid,
  .editorial-grid,
  .food-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 744px) {
  .consumer-home {
    gap: 18px;
  }

  .hero-scene {
    min-height: 620px;
    border-radius: 16px;
  }

  .hero-object,
  .hero-motion-layer {
    object-position: 60% center;
  }

  .hero-content {
    left: 18px;
    right: 18px;
    bottom: 238px;
    width: auto;
    min-width: 0;
    padding: 20px;
  }

  .hero-content h1 {
    font-size: 34px;
  }

  .hero-copy {
    font-size: 16px;
  }

  .hero-metrics {
    left: 20px;
    right: 20px;
    bottom: 20px;
    grid-template-columns: 1fr;
    max-width: none;
  }

  .hero-metrics article {
    min-height: 68px;
  }

  .search-entry {
    align-items: stretch;
    flex-direction: column;
  }

  .experience-grid,
  .editorial-grid,
  .food-grid {
    grid-template-columns: 1fr;
  }

  .experience-card {
    min-height: auto;
  }

  .section-heading h2 {
    font-size: 26px;
  }
}
</style>
