<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import DemandesBoard from '../../components/demande/DemandesBoard.vue'
import { demandes } from '../../data/demandes'

// Demandes à traiter : en attente à gauche (ouvre l'écran de validation de l'étape), déjà faites à droite (ouvre la fiche)
const versValidation = (d) => `/validation/${d.etape}?id=${d.id}`
</script>

<template>
  <AppLayout title="Demandes à traiter">
    <DemandesBoard
      :demandes="demandes"
      titre-en-cours="En attente"
      titre-traitees="Déjà faites"
      :lien-en-cours="versValidation"
    />
  </AppLayout>
</template>
