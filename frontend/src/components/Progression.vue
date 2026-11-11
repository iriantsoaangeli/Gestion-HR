<script setup>
import { computed } from 'vue'
import { ETAPES } from '../data/etapes'

const props = defineProps({ etape: { type: String, required: true } })

const courante = computed(() => ETAPES.findIndex((e) => e.code === props.etape))
const etat = (i) => (i < courante.value ? 'faite' : i === courante.value ? 'courante' : 'a-venir')
</script>

<template>
  <ol class="progression">
    <li v-for="(e, i) in ETAPES" :key="e.code" :class="etat(i)">
      <span class="point"></span>
      <span class="label">{{ e.label }}</span>
    </li>
  </ol>
</template>
