<script setup>
import AppLayout from '../components/AppLayout.vue'
import Progression from '../components/Progression.vue'
import DemandeForm from '../components/DemandeForm.vue'

// Le manager crée la demande ; la validation se fait dans ValidationView (autres utilisateurs)
</script>

<template>
  <AppLayout title="Demande de personnel">
    <Progression etape="manager" />
    <DemandeForm />
  </AppLayout>
</template>
