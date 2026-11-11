<script setup>
import { computed } from 'vue'
import DemandesColonne from './DemandesColonne.vue'
import { ETAPES } from '../data/etapes'

// Deux tableaux côte à côte : « En cours » à gauche, « Déjà traitées » à droite.
// Utilisé par Mes demandes et Toutes les demandes (seule la liste passée change).
const props = defineProps({ demandes: { type: Array, required: true } })

// Manager crée la demande : les attentes commencent à HR
const FILTRES_EN_COURS = ETAPES.slice(1).map((e) => ({ code: e.code, label: e.label }))
const FILTRES_TRAITEES = [
  { code: 'approuvee', label: 'Terminées' },
  { code: 'refusee', label: 'Refusées' },
]
const TRAITEES = ['approuvee', 'refusee']

const enCours = computed(() => props.demandes.filter((d) => !TRAITEES.includes(d.etape)))
const traitees = computed(() => props.demandes.filter((d) => TRAITEES.includes(d.etape)))
</script>

<template>
  <div class="board">
    <DemandesColonne titre="En cours" :demandes="enCours" :filtres="FILTRES_EN_COURS" />
    <DemandesColonne titre="Déjà traitées" :demandes="traitees" :filtres="FILTRES_TRAITEES" />
  </div>
</template>
