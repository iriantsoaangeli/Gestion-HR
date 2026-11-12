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
    meta: { acces: (u) => u.peutCreerDemande }, // MANAGER
    component: () => import('../views/demande/NouvelleDemandeView.vue'),
  },
  {
    // Demandes créées par l'utilisateur connecté (accueil du MANAGER)
    path: '/mes-demandes',
    name: 'mes-demandes',
    meta: { acces: (u) => u.peutCreerDemande }, // MANAGER
    component: () => import('../views/demande/MesDemandesView.vue'),
  },
  {
    path: '/toutes-les-demandes',
    name: 'toutes-les-demandes',
    meta: { acces: (u) => u.voitToutesLesDemandes }, // ADMIN et DIRECTION
    component: () => import('../views/demande/ToutesLesDemandesView.vue'),
  },
  {
    // À valider (mon étape) et déjà traitées par moi ; la décision se prend dans la fiche
    path: '/demandes-a-traiter',
    name: 'demandes-a-traiter',
    meta: { acces: (u) => u.valideDesDemandes }, // HR, DIRECTION, FINANCE
    component: () => import('../views/demande/DemandesATraiterView.vue'),
  },
  {
    path: '/demandes/:id(\\d+)',
    name: 'fiche-demande',
    props: true,
    component: () => import('../views/demande/FicheDemandeView.vue'),
  },
]

// Accueil selon le rôle : MANAGER ses demandes, HR / DIRECTION / FINANCE ce qu'ils ont à traiter, ADMIN tout
function accueil(u) {
  if (u.peutCreerDemande) return '/mes-demandes'
  if (u.valideDesDemandes) return '/demandes-a-traiter'
  if (u.voitToutesLesDemandes) return '/toutes-les-demandes'
  return '/login'
}

const offre = [
  {
    // Données statiques pour l'instant (api/offres.js). Rôle lu en direct : le backend n'a pas encore de droit « offres »
    path: '/offres/nouvelle',
    name: 'creation-offre',
    meta: { acces: (u) => u.role === 'HR' }, // HR uniquement
    component: () => import('../views/offre/CreationOffreView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // La page d'accueil est choisie dans beforeEach (il faut d'abord connaître l'utilisateur)
  routes: [{ path: '/', name: 'accueil', component: { render: () => null } }, ...auth, ...demande, ...offre],
})

// Il faut être connecté partout, sauf sur les pages meta.public ; une page réservée renvoie à l'accueil
router.beforeEach(async (to) => {
  await chargerSession()
  if (!utilisateur.value && !to.meta.public) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (!utilisateur.value) return
  const versAccueil = to.name === 'login' || to.name === 'accueil' || (to.meta.acces && !to.meta.acces(utilisateur.value))
  // Comparaison au chemin : un rôle sans page (accueil = /login) reste sur le login au lieu de boucler
  if (versAccueil && accueil(utilisateur.value) !== to.path) return accueil(utilisateur.value)
})

export default router
