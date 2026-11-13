<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/layout/AppLayout.vue'
import FicheOffre from '../../components/offre/FicheOffre.vue'
import Erreur from '../../components/ui/Erreur.vue'
import { offre as chargerOffre, demandesApprouvees } from '../../api/offres'
import { dossiersDeLOffre } from '../../api/dossiers'
import { entretiensDeLOffre } from '../../api/entretiens'

// Fiche d'une offre d'emploi (HR) : /offres/:id, avec ses candidats (dossiers) et ses entretiens.
//
const props = defineProps({ id: { type: String, required: true } })

const donnees = ref(null) // { offre, dossiers, entretiens, demande }
const erreur = ref('')

async function charger() {
  erreur.value = ''
  try {
    const [offre, dossiers, entretiens, demandes] = await Promise.all([
      chargerOffre(props.id),
      dossiersDeLOffre(props.id),
      entretiensDeLOffre(props.id),
      demandesApprouvees(),
    ])
    const demande = demandes.find((d) => d.id === offre.idDemande) ?? null
    donnees.value = { offre, dossiers, entretiens, demande }
  } catch (e) {
    donnees.value = null
    erreur.value = e.message
  }
}
watch(() => props.id, charger, { immediate: true })

// Retour à la page d'où l'on vient, sinon la liste des offres
const router = useRouter()
const retour = () => (window.history.state?.back ? router.back() : router.push('/offres'))
</script>

<template>
  <AppLayout>
    <template v-if="!donnees">
      <h1>Fiche offre</h1>
      <Erreur :message="erreur">
        <RouterLink to="/offres">Retour aux offres</RouterLink>
      </Erreur>
      <p v-if="!erreur" class="help">Chargement…</p>
    </template>

    <FicheOffre
      v-else
      :offre="donnees.offre"
      :dossiers="donnees.dossiers"
      :entretiens="donnees.entretiens"
      :demande="donnees.demande"
    >
      <footer class="fiche-actions">
        <a href="#" @click.prevent="retour">← Retour</a>
      </footer>
    </FicheOffre>
  </AppLayout>
</template>
