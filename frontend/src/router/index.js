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
  {
    // Offres ouvertes / closes. Données statiques pour l'instant (api/offres.js, dossiers.js, entretiens.js)
    path: '/offres',
    name: 'offres',
    meta: { acces: (u) => u.role === 'HR' }, // HR uniquement
    component: () => import('../views/offre/OffresView.vue'),
  },
  {
    // Fiche d'une offre : candidats (dossiers) et entretiens
    path: '/offres/:id(\\d+)',
    name: 'fiche-offre',
    props: true,
    meta: { acces: (u) => u.role === 'HR' }, // HR uniquement
    component: () => import('../views/offre/FicheOffreView.vue'),
  },
]

const dossier = [
  {
    // Dépôt de dossier (bloc 2 : « Dépôt de dossier : hr »). Données statiques pour l'instant (api/dossiers.js)
    path: '/dossiers/nouveau',
    name: 'depot-dossier',
    meta: { acces: (u) => u.role === 'HR' }, // HR uniquement
    component: () => import('../views/dossier/DepotDossierView.vue'),
  },
]

const entretien = [
  {
    // Liste des entretiens : en attente / finis. Données statiques pour l'instant (api/entretiens.js)
    path: '/entretiens',
    name: 'entretiens',
    meta: { acces: (u) => ['DIRECTION', 'MANAGER', 'HR'].includes(u.role) },
    component: () => import('../views/entretien/EntretiensView.vue'),
  },
  {
    // Mes évaluations : à faire (je suis interviewer, pas encore évalué) / déjà faites
    path: '/evaluations',
    name: 'evaluations-a-faire',
    meta: { acces: (u) => ['DIRECTION', 'MANAGER', 'HR'].includes(u.role) },
    component: () => import('../views/entretien/EvaluationsAFaireView.vue'),
  },
  {
    // Fiche d'une évaluation : note, remarques, entretien évalué, autres évaluations
    path: '/evaluations/:id(\\d+)',
    name: 'fiche-evaluation',
    props: true,
    meta: { acces: (u) => ['DIRECTION', 'MANAGER', 'HR'].includes(u.role) },
    component: () => import('../views/entretien/FicheEvaluationView.vue'),
  },
  {
    // Préparer un entretien (HR) : offre ouverte, candidat (dossier), date, interviewers.
    // ?offre=<id>&dossier=<id> : choix déjà faits. Données statiques pour l'instant (api/entretiens.js)
    path: '/entretiens/nouveau',
    name: 'preparation-entretien',
    meta: { acces: (u) => u.role === 'HR' }, // HR uniquement
    props: (route) => ({ offre: route.query.offre ?? null, dossier: route.query.dossier ?? null }),
    component: () => import('../views/entretien/PreparationEntretienView.vue'),
  },
  {
    // Fiche d'un entretien : infos, interviewers, évaluations, bouton « Évaluer »
    path: '/entretiens/:id(\\d+)',
    name: 'fiche-entretien',
    props: true,
    meta: { acces: (u) => ['DIRECTION', 'MANAGER', 'HR'].includes(u.role) },
    component: () => import('../views/entretien/FicheEntretienView.vue'),
  },
  {
    // Évaluation d'un entretien par l'interviewer connecté (une par interviewer)
    path: '/entretiens/:id(\\d+)/evaluation',
    name: 'evaluation-entretien',
    props: true,
    meta: { acces: (u) => ['DIRECTION', 'MANAGER', 'HR'].includes(u.role) },
    component: () => import('../views/entretien/EvaluationView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // La page d'accueil est choisie dans beforeEach (il faut d'abord connaître l'utilisateur)
  routes: [
    { path: '/', name: 'accueil', component: { render: () => null } },
    ...auth,
    ...demande,
    ...offre,
    ...dossier,
    ...entretien,
  ],
})

// Il faut être connecté partout, sauf sur les pages meta.public ; une page réservée renvoie à l'accueil
router.beforeEach(async (to) => {
  await chargerSession()
  if (!utilisateur.value && !to.meta.public) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (!utilisateur.value) return
  const versAccueil =
    to.name === 'login' ||
    to.name === 'accueil' ||
    (to.meta.acces && !to.meta.acces(utilisateur.value))
  // Comparaison au chemin : un rôle sans page (accueil = /login) reste sur le login au lieu de boucler
  if (versAccueil && accueil(utilisateur.value) !== to.path) return accueil(utilisateur.value)
})

export default router
