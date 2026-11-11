<script setup>
import { computed } from 'vue'
import AppLayout from '../../components/layout/AppLayout.vue'
import DemandesBoard from '../../components/demande/DemandesBoard.vue'
import { demandes, estAMoi } from '../../data/demandes'

// Seulement les demandes créées par l'utilisateur connecté
const mesDemandes = computed(() => demandes.value.filter(estAMoi))
</script>

<template>
  <AppLayout title="Mes demandes">
    <DemandesBoard :demandes="mesDemandes" />
  </AppLayout>
</template>
