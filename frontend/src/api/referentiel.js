// Listes de choix des formulaires (/api/referentiel) : [{ id, label }]
import { appel } from './client'

export const departements = () => appel('GET', '/api/referentiel/departements')
// Département du compte connecté { id, label } : un manager ne demande que pour celui-ci
export const monDepartement = () => appel('GET', '/api/referentiel/mon-departement')
export const fonctions = () => appel('GET', '/api/referentiel/fonctions')
// Postes que je peux faire remplacer (sans ceux des autres comptes : managers, supérieurs)
export const postes = (idDepartement) => appel('GET', `/api/referentiel/postes?departement=${idDepartement}`)
