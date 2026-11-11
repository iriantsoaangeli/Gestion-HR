<script setup>
import AppLayout from '../components/AppLayout.vue'
import Progression from '../components/Progression.vue'
import DemandeForm from '../components/DemandeForm.vue'
import DemandeValidation from '../components/DemandeValidation.vue'

// Manager crée la demande, les étapes suivantes la valident ou la refusent
defineProps({ etape: { type: String, required: true } })
</script>

<template>
  <AppLayout title="Demande de personnel">
    <Progression :etape="etape" />
    <DemandeForm v-if="etape === 'manager'" />
    <DemandeValidation v-else :etape="etape" />
  </AppLayout>
</template>
