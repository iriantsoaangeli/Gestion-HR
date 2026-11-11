// Données provisoires en attendant l'API (tables departement, poste, affectation)
export const departements = [
  { id: 1, label: 'Ressources humaines' },
  { id: 2, label: 'Informatique' },
  { id: 3, label: 'Finance' },
]

export const postes = [
  { id: 1, label: 'Chargé de recrutement', idDepartement: 1 },
  { id: 2, label: 'Gestionnaire paie', idDepartement: 1 },
  { id: 3, label: 'Développeur', idDepartement: 2 },
  { id: 4, label: 'Administrateur système', idDepartement: 2 },
  { id: 5, label: 'Comptable', idDepartement: 3 },
]

// Utilisateur connecté (en attendant l'authentification) : managers 1 et 2
export const UTILISATEUR_COURANT = 1
