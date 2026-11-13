// Entretiens : qui peut évaluer, et l'heure d'une date-heure de l'API
// (utilisé par components/entretien/FicheEntretien.vue et EvaluationForm.vue).

// Lu sur les drapeaux de l'API pour l'utilisateur connecté (monPoste, raisonEvaluer).
// Renvoie { moi: interviewer ou null, raison: '' si je peux évaluer, sinon pourquoi pas }.
export function droitEvaluer(entretien) {
  const moi = entretien.interviewers.find((i) => i.idPoste === entretien.monPoste) ?? null
  return { moi, raison: entretien.raisonEvaluer ?? '' }
}

// « 2026-11-10T08:30:00 » → « 08:30 »
export const heure = (texte) =>
  new Date(texte).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
