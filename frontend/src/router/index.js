import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    { path: '/demande', redirect: '/demande/manager' },
    {
      path: '/mes-demandes',
      name: 'mes-demandes',
      component: () => import('../views/MesDemandesView.vue'),
    },
    {
      // Fiche détaillée d'une demande
      path: '/demandes/:id(\\d+)',
      name: 'demande-detail',
      component: () => import('../views/DemandeDetailView.vue'),
      props: true,
    },
    {
      // Workflow : /demande/manager (formulaire), /hr, /direction, /finance (validation)
      path: '/demande/:etape(manager|hr|direction|finance)',
      name: 'demande',
      component: () => import('../views/DemandeView.vue'),
      props: true,
    },
    // TEMPORAIRE : pages de démonstration des erreurs — à supprimer avec views/temporaire/
    {
      path: '/temporaire/erreur-login',
      component: () => import('../views/temporaire/ErreurLoginView.vue'),
    },
    {
      path: '/temporaire/erreur-demande',
      component: () => import('../views/temporaire/ErreurDemandeView.vue'),
    },
    {
      path: '/temporaire/erreur-validation',
      component: () => import('../views/temporaire/ErreurValidationView.vue'),
    },
    {
      // A EFFACER avec views/AboutView.vue
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
