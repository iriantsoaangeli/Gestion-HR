import { ref } from 'vue'
import * as api from './api'

// Utilisateur connecté { id, login, role, roleLibelle } renvoyé par le backend, ou null. Partagé par toute l'application.
export const utilisateur = ref(null)

let chargement = null

// Demande une seule fois au backend si une session existe (au premier affichage ou après un F5)
export function chargerSession() {
  chargement ??= api
    .moi()
    .then((u) => (utilisateur.value = u))
    .catch(() => (utilisateur.value = null))
  return chargement
}

export async function seConnecter(login, mdp) {
  utilisateur.value = await api.login(login, mdp)
  chargement = Promise.resolve(utilisateur.value)
  return utilisateur.value
}

export async function seDeconnecter() {
  try {
    await api.logout()
  } finally {
    // Même si le serveur ne répond pas, on oublie l'utilisateur côté front
    utilisateur.value = null
    chargement = Promise.resolve(null)
  }
}
