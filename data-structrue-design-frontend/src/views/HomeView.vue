<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTopFoods } from '../api/travel'

const foods = ref([])
const router = useRouter()

const loadFoods = async () => {
  const { data } = await getTopFoods(5)
  foods.value = data
}

const goDestinations = () => {
  router.push('/destinations')
}

onMounted(loadFoods)
</script>

<template>
  <section class="home-page">
    <el-card class="module-card hero-card">
      <div>
        <h1>Find your next stay with 智能体旅游系统</h1>
        <p>像浏览旅行杂志一样探索推荐目的地、路线规划、日记与多人协作行程。</p>
      </div>
      <el-button type="primary" size="large" @click="goDestinations">开始探索</el-button>
    </el-card>

    <section class="listings">
      <article class="listing-card" v-for="item in foods" :key="item.id">
        <div class="listing-media">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
          <div v-else class="image-placeholder">暂无图片</div>
        </div>
        <div class="listing-body">
          <h3>{{ item.name }}</h3>
          <p>{{ item.cuisine }} · 评分 {{ item.rating }}</p>
          <strong>精选推荐</strong>
        </div>
      </article>
      <el-empty v-if="!foods.length" description="暂无推荐数据" />
    </section>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.hero-card {
  border-radius: 32px;
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.hero-card h1 {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.43;
  letter-spacing: -0.18px;
  color: #222222;
}

.hero-card p {
  margin-top: 8px;
  max-width: 720px;
  color: #6a6a6a;
  font-size: 14px;
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
}

.listing-media {
  position: relative;
  aspect-ratio: 16 / 10;
}

.listing-media::after {
  content: '♡';
  position: absolute;
  right: 10px;
  top: 10px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: rgba(0, 0, 0, 0.08) 0px 4px 12px;
  color: #222222;
  font-size: 16px;
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
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
