import { createRouter, createWebHistory, Router } from 'vue-router'
import CloudPlugin from '../components/CloudPlugin.vue'

const routes = [
  {
    path: '/',
    name: 'CloudPlugin',
    component: CloudPlugin,
  },
]

const VRouter: Router = (window as any).VRouter
if (VRouter) {
  for (const route of routes) {
    const { path, name, component } = route
    VRouter.addRoute('Plugin', { path: path.slice(1), name, component })
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
