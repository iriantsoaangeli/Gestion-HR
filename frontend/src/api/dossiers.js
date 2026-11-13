// Appels à l'API des dossiers de candidature (/api/dossiers), voir doc/routes/api/dossiers.md
import { appel } from './client'
import { offres } from './offres'

// Personnes de la base : poste / departement = poste occupé (employé), null = externe
export const personnes = () => appel('GET', '/api/dossiers/personnes')
export const offresOuvertes = () => offres().then((liste) => liste.filter((o) => o.ouverte))

export const dossiers = () => appel('GET', '/api/dossiers')
export const dossiersDeLOffre = (idOffre) => appel('GET', `/api/dossiers?offre=${idOffre}`)

// Lien de téléchargement d'une pièce jointe (même origine : le cookie de session suit)
export const lienPieceJointe = (id) => `/api/dossiers/pieces-jointes/${id}`

// Dépôt (multipart) : candidat = { idPersonne } ou { nom, prenom, naissance, adresse, tel, email },
// fichiers = File[]
export function deposer(idOffre, candidat, fichiers) {
  const donnees = new FormData()
  donnees.append('idOffre', idOffre)
  for (const [cle, valeur] of Object.entries(candidat)) donnees.append(cle, valeur)
  for (const f of fichiers) donnees.append('fichiers', f)
  return appel('POST', '/api/dossiers', donnees)
}
