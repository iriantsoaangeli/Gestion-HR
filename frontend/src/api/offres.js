// Appels à l'API des offres d'emploi (/api/offres), voir doc/routes/api/offres.md
import { appel } from './client'

// Demandes approuvées, chaque ligne avec idOffre (offre qui la couvre déjà, null si libre)
export const demandesApprouvees = () => appel('GET', '/api/offres/demandes-approuvees')
// { idDemande, idFonction | idPoste, intitule, description, profil, delai }
export const publier = (offre) => appel('POST', '/api/offres', offre)

export const offres = () => appel('GET', '/api/offres')
export const offre = (id) => appel('GET', `/api/offres/${id}`)
