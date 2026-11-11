import { createRouter, createWebHistory } from 'vue-router'
import { chargerSession, utilisateur } from '../auth/session'

// Une route = une vue de views/<dossier>/<Nom>View.vue (même découpage que components/)
// meta.public : accessible sans être connecté
// meta.acces(utilisateur) : page réservée, d'après ce que le backend renvoie sur l'utilisateur

const auth = [
  {
    path: '/login',
    name: 'login',
    meta: { public: true },
    component: () => import('../views/auth/LoginView.vue'),
  },
]

const demande = [
  {
    path: '/demande',
    name: 'nouvelle-demande',
    component: () => import('../views/demande/NouvelleDemandeView.vue'),
  },
  {
    // Demandes créées par l'utilisateur connecté (c'est aussi l'accueil)
    path: '/mes-demandes',
    name: 'mes-demandes',
    component: () => import('../views/demande/MesDemandesView.vue'),
  },
  {
    path: '/toutes-les-demandes',
    name: 'toutes-les-demandes',
    meta: { acces: (u) => u.voitToutesLesDemandes }, // ADMIN et DIRECTION
    component: () => import('../views/demande/ToutesLesDemandesView.vue'),
  },
  {
    // En attente (→ écran de validation) et déjà faites
    path: '/demandes-a-traiter',
    name: 'demandes-a-traiter',
    component: () => import('../views/demande/DemandesATraiterView.vue'),
  },
  {
    path: '/demandes/:id(\\d+)',
    name: 'fiche-demande',
    props: true,
    component: () => import('../views/demande/FicheDemandeView.vue'),
  },
  {
    // Une page par étape, chacune tenue par ses propres utilisateurs
    path: '/validation/:etape(hr|direction|finance)',
    name: 'validation',
    props: true,
    component: () => import('../views/demande/ValidationView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [{ path: '/', name: 'accueil', redirect: '/mes-demandes' }, ...auth, ...demande],
})

// Il faut être connecté partout, sauf sur les pages meta.public ; une page réservée renvoie à l'accueil
router.beforeEach(async (to) => {
  await chargerSession()
  if (!utilisateur.value && !to.meta.public) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (utilisateur.value && to.name === 'login') return '/'
  if (to.meta.acces && !to.meta.acces(utilisateur.value)) return '/'
})

export default router
