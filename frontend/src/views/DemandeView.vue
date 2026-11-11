<script setup>
import AppLayout from '../components/AppLayout.vue'
import Progression from '../components/Progression.vue'
import DemandeForm from '../components/DemandeForm.vue'
import DemandeValidation from '../components/DemandeValidation.vue'

// Manager crée la demande, les étapes suivantes la valident ou la refusent
defineProps({ etape: { type: String, required: true } })
</script>

<template>
  <!-- En validation, le titre est dans l'en-tête de la fiche -->
  <AppLayout :title="etape === 'manager' ? 'Demande de personnel' : ''">
    <!-- La validation affiche sa propre progression (celle de la demande) dans la fiche -->
    <template v-if="etape === 'manager'">
      <Progression etape="manager" />
      <DemandeForm />
    </template>
    <DemandeValidation v-else :etape="etape" />
  </AppLayout>
</template>
