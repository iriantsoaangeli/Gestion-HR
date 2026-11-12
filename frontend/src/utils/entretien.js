// Entretiens : qui peut évaluer, et l'heure d'une date-heure de l'API
// (utilisé par components/entretien/FicheEntretien.vue et EvaluationForm.vue).

// DONNÉES STATIQUES : le compte connecté n'a pas d'idPoste, on retrouve son poste d'interviewer
// par son login (api/entretiens.js). À remplacer par ce que renverra le backend.
// Renvoie { moi: interviewer ou null, raison: '' si je peux évaluer, sinon pourquoi pas }.
export function droitEvaluer(entretien, utilisateur) {
  const moi = entretien.interviewers.find((i) => i.login === utilisateur?.login) ?? null
  if (!moi) return { moi, raison: "Vous n'êtes pas interviewer de cet entretien." }
  if (entretien.evaluations.some((v) => v.idPoste === moi.idPoste))
    return { moi, raison: 'Vous avez déjà évalué cet entretien.' }
  return { moi, raison: '' }
}

// « 2026-11-10T08:30:00 » → « 08:30 »
export const heure = (texte) =>
  new Date(texte).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
