// Appels à l'API des entretiens et évaluations (/api/entretiens, /api/evaluations),
// voir doc/routes/api/entretiens.md. Les notes séparées (notesCachees) et les drapeaux de
// l'utilisateur connecté (monPoste, peutEvaluer, raisonEvaluer, maNote) sont calculés par le backend.
import { appel } from './client'

export const enAttente = () => appel('GET', '/api/entretiens/en-attente')
export const finis = () => appel('GET', '/api/entretiens/finis')
export const entretiensDeLOffre = (idOffre) => appel('GET', `/api/entretiens?offre=${idOffre}`)
export const entretien = (id) => appel('GET', `/api/entretiens/${id}`)

// Postes qui peuvent faire un entretien (formulaire de préparation, HR)
export const interviewersPossibles = () => appel('GET', '/api/entretiens/interviewers-possibles')

// Préparer un entretien (HR) : { idDossier, date: 'AAAA-MM-JJTHH:MM:00', interviewers: [idPoste] }
export const preparer = (entretien) => appel('POST', '/api/entretiens', entretien)

// Évaluer un entretien en tant qu'interviewer connecté : { note, remarques }.
// Renvoie l'entretien avec idEvaluation (pour ouvrir sa fiche).
export const evaluer = (idEntretien, evaluation) =>
  appel('POST', `/api/entretiens/${idEntretien}/evaluation`, evaluation)

// Mes évaluations : à faire (je suis interviewer, pas encore évalué) et faites (avec maNote)
export const aEvaluer = () => appel('GET', '/api/evaluations/a-faire')
export const evaluesParMoi = () => appel('GET', '/api/evaluations/faites')

// Une évaluation avec son évaluateur et l'entretien évalué (403 si les notes me sont cachées)
export const evaluation = (id) => appel('GET', `/api/evaluations/${id}`)
