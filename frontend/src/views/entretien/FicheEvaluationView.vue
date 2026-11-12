<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/layout/AppLayout.vue'
import FicheEvaluation from '../../components/entretien/FicheEvaluation.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/entretiens'

// Fiche d'une évaluation : /evaluations/:id. Données statiques pour l'instant (api/entretiens.js).
const props = defineProps({ id: { type: String, required: true } })

const evaluation = ref(null)
const erreur = ref('')

async function charger() {
  erreur.value = ''
  try {
    evaluation.value = await api.evaluation(props.id)
  } catch (e) {
    evaluation.value = null
    erreur.value = e.message
  }
}
watch(() => props.id, charger, { immediate: true })

// Retour à la page d'où l'on vient, sinon mes évaluations
const router = useRouter()
const retour = () => (window.history.state?.back ? router.back() : router.push('/evaluations'))
</script>

<template>
  <AppLayout>
    <template v-if="!evaluation">
      <h1>Fiche évaluation</h1>
      <Erreur :message="erreur">
        <RouterLink to="/evaluations">Retour à mes évaluations</RouterLink>
      </Erreur>
      <p v-if="!erreur" class="help">Chargement…</p>
    </template>

    <FicheEvaluation v-else :evaluation="evaluation">
      <footer class="fiche-actions">
        <a href="#" @click.prevent="retour">← Retour</a>
        <RouterLink to="/evaluations">Évaluations à faire →</RouterLink>
      </footer>
    </FicheEvaluation>
  </AppLayout>
</template>
