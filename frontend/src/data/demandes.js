import { ref } from 'vue'
import { departements, postes } from './mock'

// Demandes en mémoire, en attendant l'API.
// ajouts : postes à créer [{ label, effectif }] — l'effectif ne concerne QUE la création
// remplacements : id des postes existants à remplir (1 poste = 1 siège, pas d'effectif)
// En base, chaque poste (créé ou remplacé) = une ligne de demandePoste
// delai : date limite (AAAA-MM-JJ) ou null — absent = null
// etape : 'brouillon', code de l'étape qui doit la traiter, 'approuvee' ou 'refusee'
export const demandes = ref([
  { id: 1, idDepartement: 2, ajouts: [{ label: 'Développeur', effectif: 2 }], remplacements: [], motif: 'Nouveau projet web', date: '2026-11-02', delai: '2027-02-02', etape: 'hr' },
  { id: 2, idDepartement: 1, ajouts: [], remplacements: [1, 2], motif: 'Départ à la retraite', date: '2026-10-28', etape: 'direction' },
  { id: 3, idDepartement: 3, ajouts: [{ label: 'Contrôleur de gestion', effectif: 1 }], remplacements: [5], motif: 'Clôture annuelle', date: '2026-10-20', etape: 'finance' },
  { id: 4, idDepartement: 2, ajouts: [], remplacements: [4], motif: 'Remplacement', date: '2026-10-05', etape: 'approuvee' },
  { id: 5, idDepartement: 1, ajouts: [{ label: 'Gestionnaire paie', effectif: 3 }], remplacements: [], motif: 'Surcroît d\'activité', date: '2026-09-30', etape: 'refusee', refuseePar: 'Direction' },
])

export function ajouterDemande(demande) {
  const id = Math.max(0, ...demandes.value.map((d) => d.id)) + 1
  demandes.value.push({ ...demande, id, etape: 'hr' })
  return id
}

export const trouverDemande = (id) => demandes.value.find((d) => d.id === id)

export const labelDepartement = (d) => departements.find((x) => x.id === d.idDepartement)?.label
export const labelPoste = (idPoste) => postes.find((p) => p.id === idPoste)?.label

// « Développeur ×2, Comptable ×1 »
export const labelAjouts = (d) => d.ajouts.map((a) => `${a.label} ×${a.effectif}`).join(', ')
export const labelRemplacements = (d) => d.remplacements.map(labelPoste).join(', ')
