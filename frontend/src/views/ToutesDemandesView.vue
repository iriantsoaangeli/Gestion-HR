<script setup>
import AppLayout from '../components/AppLayout.vue'
import DemandesBoard from '../components/DemandesBoard.vue'
import { demandes } from '../data/demandes'
</script>

<template>
  <AppLayout title="Toutes les demandes">
    <DemandesBoard :demandes="demandes" />
  </AppLayout>
</template>
