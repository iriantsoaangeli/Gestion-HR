<script setup>
import { computed } from 'vue'

// Boutons de filtre (puces) au-dessus d'un tableau : « Tous » + un bouton par valeur, avec leur nombre.
// v-model : la valeur choisie ('' = tous). Le parent filtre lui-même ses lignes avec cette valeur.
// elements : les lignes à compter ; code(ligne) : la valeur d'une ligne (par défaut son département) ;
// libelle(ligne) : le texte du bouton (par défaut la valeur, ex. (d) => d.statutLibelle pour un statut).
// Boutons construits à partir des valeurs présentes (une ligne sans valeur ne compte que dans « Tous »),
// triés par ordre alphabétique (ordreAlphabetique: false = ordre d'apparition), ou liste fixe `options`
// [{ code, label }] (ex. « Sans entretien » / « Avec entretien »).
const props = defineProps({
  elements: { type: Array, required: true },
  code: { type: Function, default: (x) => x.departement },
  libelle: { type: Function, default: null },
  options: { type: Array, default: null },
  ordreAlphabetique: { type: Boolean, default: true },
  tous: { type: String, default: 'Tous' },
})
const choix = defineModel({ type: String, default: '' })

const puces = computed(() => {
  if (props.options) return props.options
  const vus = new Map()
  props.elements.forEach((x) => {
    const c = props.code(x)
    if (c !== null && c !== undefined && c !== '' && !vus.has(c))
      vus.set(c, props.libelle ? props.libelle(x) : String(c))
  })
  const liste = [...vus].map(([code, label]) => ({ code, label }))
  return props.ordreAlphabetique
    ? liste.sort((a, b) => a.label.localeCompare(b.label, 'fr'))
    : liste
})

const nombre = (code) => props.elements.filter((x) => !code || props.code(x) === code).length
</script>

<template>
  <div class="puces" role="group">
    <button
      v-for="p in [{ code: '', label: tous }, ...puces]"
      :key="p.code"
      type="button"
      class="puce"
      :class="{ active: choix === p.code }"
      :aria-pressed="choix === p.code"
      @click="choix = p.code"
    >
      {{ p.label }} <span class="count">{{ nombre(p.code) }}</span>
    </button>
  </div>
</template>
