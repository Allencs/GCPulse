import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import AnalysisResult from '../views/AnalysisResult.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/result',
    name: 'AnalysisResult',
    component: AnalysisResult
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

