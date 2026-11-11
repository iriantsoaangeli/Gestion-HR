<script setup>
import { computed } from 'vue'
import { ETAPES } from '../../data/etapes'

// etape : code de l'étape en cours, 'approuvee' (tout est fait)
// refusee : code de l'étape qui a refusé (marquée en rouge, la suite reste à venir)
const props = defineProps({
  etape: { type: String, required: true },
  refusee: { type: String, default: '' },
})

const courante = computed(() => {
  if (props.etape === 'approuvee') return ETAPES.length
  return ETAPES.findIndex((e) => e.code === (props.refusee || props.etape))
})

function etat(i) {
  if (i < courante.value) return 'faite'
  if (i === courante.value) return props.refusee ? 'refusee' : 'courante'
  return 'a-venir'
}
</script>

<template>
  <ol class="progression">
    <li v-for="(e, i) in ETAPES" :key="e.code" :class="etat(i)">
      <span class="point"></span>
      <span class="label">{{ e.label }}</span>
    </li>
  </ol>
</template>
