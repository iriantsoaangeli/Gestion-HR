<script setup>
import AppLayout from '../components/layout/AppLayout.vue'
import Progression from '../components/ui/Progression.vue'
import DemandeForm from '../components/demande/DemandeForm.vue'

// Le manager crée la demande ; la validation se fait dans ValidationView (autres utilisateurs)
</script>

<template>
  <AppLayout title="Demande de personnel">
    <Progression etape="manager" />
    <DemandeForm />
  </AppLayout>
</template>
