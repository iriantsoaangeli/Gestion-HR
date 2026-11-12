<script setup>
// Champ de recherche au-dessus d'un tableau, à côté des puces (FiltrePuces).
// v-model : le texte cherché. Le parent filtre ses lignes avec contient() de utils/texte.js
// (insensible à la casse et aux accents). placeholder, aria-label… vont sur le champ.
const texte = defineModel({ type: String, default: '' })
</script>

<template>
  <input v-model="texte" type="search" class="filtre-recherche" />
</template>
