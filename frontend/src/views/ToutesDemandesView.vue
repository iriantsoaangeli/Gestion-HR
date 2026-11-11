<script setup>
import AppLayout from '../components/layout/AppLayout.vue'
import DemandesBoard from '../components/demande/DemandesBoard.vue'
import { demandes } from '../data/demandes'
</script>

<template>
  <AppLayout title="Toutes les demandes">
    <DemandesBoard :demandes="demandes" />
  </AppLayout>
</template>
