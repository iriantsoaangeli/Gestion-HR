<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/layout/AppLayout.vue'
import FicheDemande from '../../components/demande/FicheDemande.vue'
import DecisionDemande from '../../components/demande/DecisionDemande.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/demandes'

// Fiche d'une demande : /demandes/:id. Si c'est à moi de décider, le panneau Valider / Refuser s'affiche.
const props = defineProps({ id: { type: String, required: true } })

const demande = ref(null)
const erreur = ref('')
// Garde le panneau de décision affiché après ma décision (message de confirmation)
const jaiDecide = ref(false)

async function charger() {
  erreur.value = ''
  jaiDecide.value = false
  try {
    demande.value = await api.detail(props.id)
  } catch (e) {
    demande.value = null
    erreur.value = e.message
  }
}
watch(() => props.id, charger, { immediate: true })

function apresDecision(d) {
  demande.value = d
  jaiDecide.value = true
}

// Retour à la page d'où l'on vient, sinon l'accueil
const router = useRouter()
const retour = () => (window.history.state?.back ? router.back() : router.push('/'))
</script>

<template>
  <AppLayout>
    <template v-if="!demande">
      <h1>Fiche demande</h1>
      <Erreur :message="erreur">
        <RouterLink to="/">Retour à l'accueil</RouterLink>
      </Erreur>
      <p v-if="!erreur" class="help">Chargement…</p>
    </template>

    <FicheDemande v-else :demande="demande">
      <template v-if="demande.aMoiDeDecider || jaiDecide" #cote>
        <DecisionDemande :demande="demande" @decidee="apresDecision" />
      </template>

      <footer class="fiche-actions">
        <a href="#" @click.prevent="retour">← Retour</a>
      </footer>
    </FicheDemande>
  </AppLayout>
</template>
