<script setup>
import { computed } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import DemandesColonne from '../components/DemandesColonne.vue'
import { demandes } from '../data/demandes'
import { ETAPES } from '../data/etapes'

// Manager crée la demande : les attentes commencent à HR
const attentes = ETAPES.slice(1)
const TRAITEES = ['approuvee', 'refusee']

const enCours = computed(() => demandes.value.filter((d) => !TRAITEES.includes(d.etape)))
const traitees = computed(() => demandes.value.filter((d) => TRAITEES.includes(d.etape)))
</script>

<template>
  <AppLayout title="Mes demandes">
    <div class="board">
      <DemandesColonne
        titre="En cours"
        :demandes="enCours"
        :filtres="attentes.map((e) => ({ code: e.code, label: e.label }))"
      />
      <DemandesColonne
        titre="Déjà traitées"
        :demandes="traitees"
        :filtres="[
          { code: 'approuvee', label: 'Terminées' },
          { code: 'refusee', label: 'Refusées' },
        ]"
      />
    </div>
  </AppLayout>
</template>
