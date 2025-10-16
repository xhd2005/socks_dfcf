import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import StockListView from '../views/StockListView.vue'
import RecommendView from '../views/RecommendView.vue'
import AIAnalysisView from '../views/AIAnalysisView.vue'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/stocks',
      name: 'stocks',
      component: StockListView
    },
    {
      path: '/recommend',
      name: 'recommend',
      component: RecommendView
    },
    {
      path: '/ai-analysis',
      name: 'ai-analysis',
      component: AIAnalysisView
    }
  ]
})

export default router