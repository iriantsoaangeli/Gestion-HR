<script setup>
import AppLayout from '../components/AppLayout.vue'
import DemandeValidation from '../components/DemandeValidation.vue'

// Page de validation d'une étape (HR, Direction, Finance).
// Chaque étape est tenue par des utilisateurs différents, sur leur propre poste :
// la page ne renvoie jamais vers l'écran d'une autre étape.
defineProps({ etape: { type: String, required: true } })
</script>

<template>
  <AppLayout>
    <DemandeValidation :etape="etape" />
  </AppLayout>
</template>
