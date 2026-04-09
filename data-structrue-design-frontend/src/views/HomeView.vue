<script setup>
import { onMounted, ref } from 'vue'
import { getTopFoods } from '../api/travel'

/**
 * 首页精选美食列表：用于展示系统“推荐能力”入口。
 */
const foods = ref([])

/**
 * 拉取前 5 个美食推荐，作为首页概览信息。
 */
const loadFoods = async () => {
  const { data } = await getTopFoods(5)
  foods.value = data
}

onMounted(loadFoods)
</script>

<template>
  <section class="home-page">
    <el-card class="module-card hero-card">
      <div class="hero-text">
        <h1>基于智能体的个性化旅游系统</h1>
        <p>覆盖旅游前推荐、旅游中导航、旅游后日记与多人协作，支持景区+校园双场景。</p>
      </div>
      <el-row :gutter="12">
        <el-col :md="6" :sm="12" :xs="24">
          <el-statistic title="推荐结果" :value="10" suffix="Top-K" />
        </el-col>
        <el-col :md="6" :sm="12" :xs="24">
          <el-statistic title="路线策略" :value="4" suffix="种" />
        </el-col>
        <el-col :md="6" :sm="12" :xs="24">
          <el-statistic title="日记交流" :value="3" suffix="类型" />
        </el-col>
        <el-col :md="6" :sm="12" :xs="24">
          <el-statistic title="协作规划" :value="24" suffix="小时在线" />
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="feature-row">
      <el-col :md="12" :xs="24">
        <el-card class="module-card feature-card">
          <div class="module-header"><h2>核心能力</h2></div>
          <el-timeline>
            <el-timeline-item type="primary">目的地 Top-K 推荐 + 多关键字检索</el-timeline-item>
            <el-timeline-item type="success">室内外路线规划 + 多交通工具混合</el-timeline-item>
            <el-timeline-item type="warning">旅游日记管理 + 全文检索 + 热度评分</el-timeline-item>
            <el-timeline-item type="danger">行程共创 + 权限协作 + 攻略复刻</el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :md="12" :xs="24">
        <el-card class="module-card feature-card food-card">
          <div class="module-header"><h2>今日美食推荐</h2></div>
          <el-empty v-if="!foods.length" description="暂无推荐数据" />
          <el-space v-else direction="vertical" fill>
            <el-tag
              v-for="item in foods"
              :key="item.id"
              type="success"
              size="large"
              class="food-tag"
            >
              {{ item.name }} · {{ item.cuisine }} · 热度 {{ item.heat }}
            </el-tag>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 16px;
}

.hero-card {
  background:
    radial-gradient(circle at 90% 10%, rgba(34, 197, 94, 0.22), transparent 28%),
    radial-gradient(circle at 15% 85%, rgba(59, 130, 246, 0.22), transparent 25%),
    rgba(255, 255, 255, 0.92);
}

.hero-text {
  margin-bottom: 16px;
}

.hero-text h1 {
  color: #1e40af;
  font-size: 30px;
  margin-bottom: 8px;
}

.hero-text p {
  color: #334155;
}

.feature-card {
  min-height: 320px;
}

.food-card {
  background: linear-gradient(160deg, rgba(240, 253, 244, 0.95), rgba(236, 253, 245, 0.9));
}

.food-tag {
  width: 100%;
  justify-content: flex-start;
}
</style>
