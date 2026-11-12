import { ref } from 'vue'

// Charge des données depuis l'API avec un état « chargement » et un message d'erreur.
// const { donnees, erreur, chargement, recharger } = useChargement(() => api.mesDemandes(), [])
export function useChargement(charger, valeurInitiale = null) {
  const donnees = ref(valeurInitiale)
  const erreur = ref('')
  const chargement = ref(false)

  async function recharger() {
    chargement.value = true
    erreur.value = ''
    try {
      donnees.value = await charger()
    } catch (e) {
      erreur.value = e.message
    } finally {
      chargement.value = false
    }
  }

  recharger()
  return { donnees, erreur, chargement, recharger }
}
