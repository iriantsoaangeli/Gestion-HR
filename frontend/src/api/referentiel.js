// Listes de choix des formulaires (/api/referentiel) : [{ id, label }]
import { appel } from './client'

export const departements = () => appel('GET', '/api/referentiel/departements')
export const fonctions = () => appel('GET', '/api/referentiel/fonctions')
export const postes = (idDepartement) => appel('GET', `/api/referentiel/postes?departement=${idDepartement}`)
