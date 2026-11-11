// Appels à l'API des demandes de personnel (/api/demandes), voir doc/routes/api/demandes.md
import { appel } from './client'

// { idDepartement, motif, delai, creations: [{ idFonction, effectif }], remplacements: [idPoste] }
export const creer = (demande) => appel('POST', '/api/demandes', demande)
export const valider = (id) => appel('POST', `/api/demandes/${id}/valider`)
export const refuser = (id) => appel('POST', `/api/demandes/${id}/refuser`)

export const detail = (id) => appel('GET', `/api/demandes/${id}`)
export const mesDemandes = () => appel('GET', '/api/demandes/mes-demandes')
export const aTraiter = () => appel('GET', '/api/demandes/a-traiter')
export const traitees = () => appel('GET', '/api/demandes/traitees')
export const toutes = () => appel('GET', '/api/demandes')

// Étapes du workflow (progression d'une demande pas encore créée)
export const etapes = () => appel('GET', '/api/demandes/etapes')
