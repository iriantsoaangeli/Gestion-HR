<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/layout/AppLayout.vue'
import EvaluationForm from '../../components/entretien/EvaluationForm.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/entretiens'

// Évaluation d'un entretien : /entretiens/:id/evaluation. Après l'enregistrement, on ouvre la fiche de mon évaluation.
//
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

const router = useRouter()
// replace : « Retour » depuis la fiche ne revient pas sur le formulaire déjà envoyé
const apresEvaluation = (e) =>
  router.replace({ name: 'fiche-evaluation', params: { id: String(e.idEvaluation) } })
</script>

<template>
  <AppLayout>
    <template v-if="!entretien">
      <h1>Évaluation d'entretien</h1>
      <Erreur :message="erreur">
        <RouterLink to="/entretiens">Retour aux entretiens</RouterLink>
      </Erreur>
      <p v-if="!erreur" class="help">Chargement…</p>
    </template>

    <EvaluationForm v-else :entretien="entretien" @evaluee="apresEvaluation" />
  </AppLayout>
</template>
