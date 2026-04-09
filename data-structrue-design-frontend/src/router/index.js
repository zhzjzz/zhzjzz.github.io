import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DestinationView from '../views/DestinationView.vue'
import RouteView from '../views/RouteView.vue'
import DiaryView from '../views/DiaryView.vue'
import ItineraryView from '../views/ItineraryView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/destinations', name: 'destinations', component: DestinationView },
  { path: '/routes', name: 'routes', component: RouteView },
  { path: '/diaries', name: 'diaries', component: DiaryView },
  { path: '/itineraries', name: 'itineraries', component: ItineraryView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
