<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import Progression from '../../components/ui/Progression.vue'
import DemandeForm from '../../components/demande/DemandeForm.vue'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'

// Nouvelle demande (MANAGER). Les étapes de la progression viennent du backend.
const { donnees: etapes } = useChargement(api.etapes, [])
</script>

<template>
  <AppLayout title="Demande de personnel">
    <Progression v-if="etapes.length" :etapes="etapes" />
    <DemandeForm />
  </AppLayout>
</template>
