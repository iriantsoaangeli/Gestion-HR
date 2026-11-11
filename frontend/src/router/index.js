import { createRouter, createWebHistory } from 'vue-router'
import { chargerSession, utilisateur } from '../auth/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // L'accueil, c'est Mes demandes
    { path: '/', name: 'home', redirect: '/mes-demandes' },
    {
      path: '/login',
      name: 'login',
      meta: { public: true },
      component: () => import('../views/LoginView.vue'),
    },
    {
      // Le manager crée une demande
      path: '/demande',
      name: 'demande',
      component: () => import('../views/DemandeView.vue'),
    },
    {
      // Demandes créées par l'utilisateur connecté
      path: '/mes-demandes',
      name: 'mes-demandes',
      component: () => import('../views/MesDemandesView.vue'),
    },
    {
      // Réservée à ADMIN et DIRECTION (le backend le dit via utilisateur.voitToutesLesDemandes)
      path: '/toutes-les-demandes',
      name: 'toutes-les-demandes',
      meta: { acces: (u) => u.voitToutesLesDemandes },
      component: () => import('../views/ToutesDemandesView.vue'),
    },
    {
      // Fiche détaillée d'une demande
      path: '/demandes/:id(\\d+)',
      name: 'demande-detail',
      component: () => import('../views/DemandeDetailView.vue'),
      props: true,
    },
    {
      // Validation par étape, chacune par ses propres utilisateurs : /validation/hr, /direction, /finance
      path: '/validation/:etape(hr|direction|finance)',
      name: 'validation',
      component: () => import('../views/ValidationView.vue'),
      props: true,
    },
    // Anciennes adresses
    { path: '/demande/manager', redirect: '/demande' },
    {
      path: '/demande/:etape(hr|direction|finance)',
      redirect: (to) => ({ path: `/validation/${to.params.etape}`, query: to.query }),
    },
    // TEMPORAIRE : pages de démonstration des erreurs — à supprimer avec views/temporaire/
    {
      path: '/temporaire/erreur-login',
      meta: { public: true },
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

// Il faut être connecté partout, sauf sur les pages marquées meta.public (login…)
router.beforeEach(async (to) => {
  await chargerSession()
  if (!utilisateur.value && !to.meta.public) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (utilisateur.value && to.name === 'login') return '/'
  // Page réservée : sans le droit, retour à l'accueil (Mes demandes)
  if (to.meta.acces && !to.meta.acces(utilisateur.value)) return '/'
})

export default router
