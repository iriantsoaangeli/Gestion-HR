import { ref } from 'vue'
import { departements, postes } from './mock'

// Demandes en mémoire, en attendant l'API.
// etape : 'brouillon', code de l'étape qui doit la traiter, 'approuvee' ou 'refusee'
export const demandes = ref([
  { id: 1, idDepartement: 2, idPostes: [3], effectif: 2, motif: 'Nouveau projet web', date: '2026-11-02', etape: 'hr' },
  { id: 2, idDepartement: 1, idPostes: [1, 2], effectif: 1, motif: 'Départ à la retraite', date: '2026-10-28', etape: 'direction' },
  { id: 3, idDepartement: 3, idPostes: [5], effectif: 1, motif: 'Clôture annuelle', date: '2026-10-20', etape: 'finance' },
  { id: 4, idDepartement: 2, idPostes: [4], effectif: 1, motif: 'Remplacement', date: '2026-10-05', etape: 'approuvee' },
  { id: 5, idDepartement: 1, idPostes: [2], effectif: 3, motif: 'Surcroît d\'activité', date: '2026-09-30', etape: 'refusee', refuseePar: 'Direction' },
])

export function ajouterDemande(demande) {
  const id = Math.max(0, ...demandes.value.map((d) => d.id)) + 1
  demandes.value.push({ ...demande, id, etape: 'hr' })
  return id
}

export const trouverDemande = (id) => demandes.value.find((d) => d.id === id)

export const labelDepartement = (d) => departements.find((x) => x.id === d.idDepartement)?.label
export const labelPostes = (d) =>
  postes.filter((p) => d.idPostes.includes(p.id)).map((p) => p.label).join(', ')
