<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import Progression from '../../components/ui/Progression.vue'
import DemandeForm from '../../components/demande/DemandeForm.vue'

// Nouvelle demande : le manager la crée ; elle est ensuite validée dans ValidationView (autres utilisateurs)
</script>

<template>
  <AppLayout title="Demande de personnel">
    <Progression etape="manager" />
    <DemandeForm />
  </AppLayout>
</template>
