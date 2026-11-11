import { ref } from 'vue'
import { departements, postes } from './mock'

// Demandes en mémoire, en attendant l'API.
// ajouts : postes à créer [{ label, effectif }] — l'effectif ne concerne QUE la création
// remplacements : id des postes existants à remplir (1 poste = 1 siège, pas d'effectif)
// En base, chaque poste (créé ou remplacé) = une ligne de demandePoste
// delai : date limite (AAAA-MM-JJ) ou null — absent = null
// historique : date de décision de chaque étape { hr: 'AAAA-MM-JJ', … } (validation ou refus)
// etape : code de l'étape qui doit la traiter, 'approuvee' ou 'refusee'
export const demandes = ref([
  { id: 1, idDepartement: 2, ajouts: [{ label: 'Développeur', effectif: 2 }], remplacements: [], motif: 'Nouveau projet web', date: '2026-11-02', delai: '2027-02-02', historique: {}, etape: 'hr' },
  { id: 2, idDepartement: 1, ajouts: [], remplacements: [1, 2], motif: 'Départ à la retraite', date: '2026-10-28', historique: { hr: '2026-10-30' }, etape: 'direction' },
  { id: 3, idDepartement: 3, ajouts: [{ label: 'Contrôleur de gestion', effectif: 1 }], remplacements: [5], motif: 'Clôture annuelle', date: '2026-10-20', historique: { hr: '2026-10-21', direction: '2026-10-27' }, etape: 'finance' },
  { id: 4, idDepartement: 2, ajouts: [], remplacements: [4], motif: 'Remplacement', date: '2026-10-05', historique: { hr: '2026-10-07', direction: '2026-10-12', finance: '2026-10-15' }, etape: 'approuvee' },
  { id: 5, idDepartement: 1, ajouts: [{ label: 'Gestionnaire paie', effectif: 3 }], remplacements: [], motif: 'Surcroît d\'activité', date: '2026-09-30', historique: { hr: '2026-10-02', direction: '2026-10-09' }, etape: 'refusee', refuseePar: 'Direction' },
  { id: 6, idDepartement: 2, ajouts: [{ label: 'Développeur', effectif: 1 }], remplacements: [3], motif: 'Application mobile', date: '2026-11-06', delai: '2026-12-15', historique: {}, etape: 'hr' },
  { id: 7, idDepartement: 3, ajouts: [], remplacements: [5], motif: 'Congé maternité', date: '2026-11-01', historique: { hr: '2026-11-03' }, etape: 'direction' },
  { id: 8, idDepartement: 1, ajouts: [{ label: 'Chargé de formation', effectif: 2 }], remplacements: [], motif: 'Plan de formation 2027', date: '2026-10-25', delai: '2027-01-31', historique: { hr: '2026-10-26', direction: '2026-11-02' }, etape: 'finance' },
  { id: 9, idDepartement: 3, ajouts: [{ label: 'Auditeur interne', effectif: 1 }], remplacements: [], motif: 'Contrôle interne', date: '2026-09-15', historique: { hr: '2026-09-16', direction: '2026-09-22', finance: '2026-09-29' }, etape: 'approuvee' },
  { id: 10, idDepartement: 2, ajouts: [{ label: 'Data analyst', effectif: 2 }], remplacements: [], motif: 'Tableaux de bord', date: '2026-09-10', historique: { hr: '2026-09-12' }, etape: 'refusee', refuseePar: 'HR' },
  { id: 11, idDepartement: 1, ajouts: [], remplacements: [1], motif: 'Mutation interne', date: '2026-08-28', historique: { hr: '2026-08-29', direction: '2026-09-04', finance: '2026-09-08' }, etape: 'approuvee' },
])

export function ajouterDemande(demande) {
  const id = Math.max(0, ...demandes.value.map((d) => d.id)) + 1
  demandes.value.push({ historique: {}, ...demande, id, etape: 'hr' })
  return id
}

export const trouverDemande = (id) => demandes.value.find((d) => d.id === id)

export const labelDepartement = (d) => departements.find((x) => x.id === d.idDepartement)?.label
export const labelPoste = (idPoste) => postes.find((p) => p.id === idPoste)?.label

// Une seule liste de postes ; chaque ligne précise si on crée ou si on remplace/remplit.
// L'effectif n'existe que pour une création (un remplacement = 1 siège existant).
export const lignesPostes = (d) => [
  ...d.ajouts.map((a) => ({ cle: `c-${a.label}`, nature: 'creation', label: a.label, effectif: a.effectif })),
  ...d.remplacements.map((id) => ({ cle: `r-${id}`, nature: 'remplacement', label: labelPoste(id), idPoste: id })),
]

export const NATURES = { creation: 'Création', remplacement: 'Remplacement' }

// « Développeur ×2 (création), Comptable (remplacement) »
export const labelPostes = (d) =>
  lignesPostes(d)
    .map((l) => `${l.label}${l.effectif ? ` ×${l.effectif}` : ''} (${NATURES[l.nature].toLowerCase()})`)
    .join(', ')

// Nombre total de postes demandés (1 poste = 1 siège) : effectifs des créations + 1 par remplacement
export const totalPostes = (d) =>
  d.ajouts.reduce((total, a) => total + a.effectif, 0) + d.remplacements.length

// Jours restants avant le délai (négatif si dépassé), null sans délai
export function joursRestants(d) {
  if (!d.delai) return null
  const aujourdhui = new Date()
  aujourdhui.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${d.delai}T00:00:00`) - aujourdhui) / 86400000)
}

// Date du jour au format AAAA-MM-JJ (heure locale)
export function aujourdhui() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
