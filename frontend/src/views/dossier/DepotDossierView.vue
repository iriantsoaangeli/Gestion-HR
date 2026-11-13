<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import DossierForm from '../../components/dossier/DossierForm.vue'

// Dépôt d'un dossier de candidature pour une offre ouverte (HR uniquement).
// offre : offre déjà choisie, lue dans la query par la route (?offre=…, depuis la fiche d'une offre).
defineProps({ offre: { type: [String, Number], default: null } })
</script>

<template>
  <AppLayout title="Dépôt de dossier">
    <DossierForm :offre="offre" />
  </AppLayout>
</template>
