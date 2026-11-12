<script setup>
import { computed } from 'vue'
import DemandesColonne from './DemandesColonne.vue'
import { estTerminee } from '../../utils/demande'

// Deux tableaux côte à côte : demandes en cours à gauche, terminées (approuvées ou refusées) à droite.
// Utilisé par Mes demandes et Toutes les demandes (seule la liste passée change).
const props = defineProps({ demandes: { type: Array, required: true } })

const enCours = computed(() => props.demandes.filter((d) => !estTerminee(d)))
const traitees = computed(() => props.demandes.filter(estTerminee))
</script>

<template>
  <div class="board">
    <DemandesColonne titre="En cours" :demandes="enCours" />
    <DemandesColonne titre="Déjà traitées" :demandes="traitees" />
  </div>
</template>
