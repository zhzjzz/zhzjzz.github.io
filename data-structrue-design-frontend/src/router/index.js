import { createRouter, createWebHistory } from "vue-router";
import { useAppStore } from "../stores/app";
import HomeView from "../views/HomeView.vue";
import DestinationView from "../views/DestinationView.vue";
import RouteView from "../views/RouteView.vue";
import DiaryView from "../views/DiaryView.vue";
import ItineraryView from "../views/ItineraryView.vue";
import LoginView from "../views/LoginView.vue";

const routes = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { public: true },
  },
  { path: "/", name: "home", component: HomeView },
  { path: "/destinations", name: "destinations", component: DestinationView },
  { path: "/routes", name: "routes", component: RouteView },
  { path: "/diaries", name: "diaries", component: DiaryView },
  { path: "/itineraries", name: "itineraries", component: ItineraryView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * 路由登录守卫：
 * - 未登录时仅允许访问登录页；
 * - 已登录后访问登录页会被重定向到首页。
 */
router.beforeEach((to) => {
  //直接放行,后续再完善登录状态的判断逻辑
  return true;
  // const appStore = useAppStore()
  // if (to.meta.public && appStore.isLoggedIn) {
  //   return { path: '/' }
  // }
  // if (!to.meta.public && !appStore.isLoggedIn) {
  //   return { path: '/login' }
  // }
  // return true
});

export default router;
