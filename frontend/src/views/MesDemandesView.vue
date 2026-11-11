<script setup>
import AppLayout from '../components/AppLayout.vue'
import DemandesColonne from '../components/DemandesColonne.vue'
import { demandes } from '../data/demandes'
import { ETAPES } from '../data/etapes'

const parEtape = (code) => demandes.value.filter((d) => d.etape === code)
// Manager crée la demande : les attentes commencent à HR
const attentes = ETAPES.slice(1)
</script>

<template>
  <AppLayout title="Mes demandes">
    <div class="board">
      <DemandesColonne titre="Brouillons" :demandes="parEtape('brouillon')" />
      <DemandesColonne
        v-for="e in attentes"
        :key="e.code"
        :titre="`En attente ${e.label}`"
        :demandes="parEtape(e.code)"
        lien
      />
      <DemandesColonne titre="Acceptées" :demandes="parEtape('approuvee')" />
      <DemandesColonne titre="Refusées" :demandes="parEtape('refusee')" refus />
    </div>
  </AppLayout>
</template>
