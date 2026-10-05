import { createRouter, createWebHashHistory } from 'vue-router'

// 注意：main.js 里没有 app.use(router)，路由实例由 App.vue 直接驱动，
// 所以这里全部用静态 import —— <component :is> 拿到的是组件对象本身。
import RoadView from '../views/RoadView.vue'
import LampView from '../views/LampView.vue'
import InspectionView from '../views/InspectionView.vue'
import RepairView from '../views/RepairView.vue'

const routes = [
  { path: '/', redirect: '/road' },
  { path: '/road', name: 'road', component: RoadView },
  { path: '/lamp', name: 'lamp', component: LampView },
  { path: '/inspection', name: 'inspection', component: InspectionView },
  { path: '/repair', name: 'repair', component: RepairView },
  { path: '/:pathMatch(.*)*', redirect: '/road' }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
