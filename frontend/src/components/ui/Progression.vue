<script setup>
// Barre de progression des étapes. etapes = [{ libelle, etat }] calculées par le backend
// (suivi d'une demande, ou GET /api/demandes/etapes) ; etat : faite, courante, refusee, a-venir.
defineProps({ etapes: { type: Array, required: true } })
</script>

<template>
  <ol class="progression">
    <li v-for="e in etapes" :key="e.libelle" :class="e.etat">
      <span class="point"></span>
      <span class="label">{{ e.libelle }}</span>
    </li>
  </ol>
</template>
