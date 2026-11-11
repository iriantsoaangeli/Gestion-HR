<script setup>
import { computed } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import FicheDemande from '../components/FicheDemande.vue'
import Erreur from '../components/Erreur.vue'
import { trouverDemande } from '../data/demandes'

// Fiche d'une demande : /demandes/:id
const props = defineProps({ id: { type: String, required: true } })

const d = computed(() => trouverDemande(Number(props.id)))
</script>

<template>
  <AppLayout>
    <template v-if="!d">
      <h1>Fiche demande</h1>
      <Erreur :message="`La demande n° ${id} n'existe pas ou a été supprimée.`">
        <RouterLink to="/mes-demandes">Voir mes demandes</RouterLink>
      </Erreur>
    </template>

    <FicheDemande v-else :demande="d">
      <footer class="fiche-actions">
        <RouterLink to="/mes-demandes">← Retour à mes demandes</RouterLink>
      </footer>
    </FicheDemande>
  </AppLayout>
</template>
