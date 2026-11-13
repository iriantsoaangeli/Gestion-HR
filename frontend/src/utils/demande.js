// Affichage d'une demande telle que renvoyée par l'API (DemandeResponse). Aucune donnée en dur :
// tout vient de la réponse, ces fonctions ne font que la mettre en forme.

// Une ligne par poste demandé : création (fonction + effectif) ou remplacement (poste existant).
// idOffre : offre qui couvre déjà la ligne (demandes approuvées de api/offres.js), sinon undefined.
export const lignesPostes = (d) => [
  ...d.creations.map((c) => ({
    cle: `c-${c.idFonction}`,
    nature: 'creation',
    label: c.fonction,
    effectif: c.effectif,
    idFonction: c.idFonction,
    idOffre: c.idOffre,
  })),
  ...d.remplacements.map((r) => ({
    cle: `r-${r.idPoste}`,
    nature: 'remplacement',
    label: r.poste,
    idPoste: r.idPoste,
    idOffre: r.idOffre,
  })),
]

export const NATURES = { creation: 'Création', remplacement: 'Remplacement' }

// Nombre de postes demandés (1 poste = 1 siège) : effectifs des créations + 1 par remplacement
export const totalPostes = (d) =>
  d.creations.reduce((t, c) => t + c.effectif, 0) + d.remplacements.length

// « Développeur ×2 (création), Comptable (remplacement) »
export const labelPostes = (d) =>
  lignesPostes(d)
    .map(
      (l) =>
        `${l.label}${l.effectif ? ` ×${l.effectif}` : ''} (${NATURES[l.nature].toLowerCase()})`,
    )
    .join(', ')

export const estTerminee = (d) => !d.etapeValideur
export const estRefusee = (d) => d.statut === 'REFUSEE'

// Libellé du statut, avec qui a refusé (dernière décision)
export function texteStatut(d) {
  if (!estRefusee(d)) return d.statutLibelle
  const refus = d.historique[d.historique.length - 1]
  return refus ? `${d.statutLibelle} par ${refus.role}` : d.statutLibelle
}

// Classe du badge : en attente, approuvée (ok), refusée (ko)
export const classeStatut = (d) => (estRefusee(d) ? 'ko' : estTerminee(d) ? 'ok' : 'attente')
