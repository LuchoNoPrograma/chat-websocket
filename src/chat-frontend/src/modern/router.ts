import { createRouter, createWebHistory } from 'vue-router';
import WelcomeView from './views/WelcomeView.vue';
import WorkspaceView from './views/WorkspaceView.vue';

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0, behavior: 'smooth' }),
  routes: [
    { path: '/', name: 'welcome', component: WelcomeView },
    { path: '/chat', name: 'chat', component: WorkspaceView },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
});
