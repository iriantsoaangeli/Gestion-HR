<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import PreparationEntretienForm from '../../components/entretien/PreparationEntretienForm.vue'

// Préparation d'un entretien (HR uniquement). offre / dossier : choix déjà faits, lus dans la query
// par la route (?offre=…&dossier=…).
defineProps({
  offre: { type: [String, Number], default: null },
  dossier: { type: [String, Number], default: null },
})
</script>

<template>
  <AppLayout title="Préparer un entretien">
    <PreparationEntretienForm :offre="offre" :dossier="dossier" />
  </AppLayout>
</template>
