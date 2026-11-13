<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/layout/AppLayout.vue'
import FicheEntretien from '../../components/entretien/FicheEntretien.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/entretiens'

// Fiche d'un entretien : /entretiens/:id.
const props = defineProps({ id: { type: String, required: true } })

const entretien = ref(null)
const erreur = ref('')

async function charger() {
  erreur.value = ''
  try {
    entretien.value = await api.entretien(props.id)
  } catch (e) {
    entretien.value = null
    erreur.value = e.message
  }
}
watch(() => props.id, charger, { immediate: true })

// Retour à la page d'où l'on vient, sinon la liste des entretiens
const router = useRouter()
const retour = () => (window.history.state?.back ? router.back() : router.push('/entretiens'))
</script>

<template>
  <AppLayout>
    <template v-if="!entretien">
      <h1>Fiche entretien</h1>
      <Erreur :message="erreur">
        <RouterLink to="/entretiens">Retour aux entretiens</RouterLink>
      </Erreur>
      <p v-if="!erreur" class="help">Chargement…</p>
    </template>

    <FicheEntretien v-else :entretien="entretien">
      <footer class="fiche-actions">
        <a href="#" @click.prevent="retour">← Retour</a>
      </footer>
    </FicheEntretien>
  </AppLayout>
</template>
