// Appels à l'API d'authentification (/api/auth). Le client HTTP commun est dans api/client.js.
import { appel } from '../api/client'

// POST /api/auth/login → { id, login, role, roleLibelle, voitToutesLesDemandes, peutCreerDemande, valideDesDemandes }
export const login = (login, mdp) => appel('POST', '/api/auth/login', { login, mdp })

// POST /api/auth/logout → ferme la session côté serveur
export const logout = () => appel('POST', '/api/auth/logout')

// GET /api/auth/moi → utilisateur connecté, ErreurApi 401 sinon
export const moi = () => appel('GET', '/api/auth/moi')
