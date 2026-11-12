<script setup>
// Champ nombre avec boutons − / + (effectif d'une demande, note d'une évaluation…).
// v-model : le nombre (null = vide ; + part alors de min). On peut aussi le taper au clavier.
// min / max : bornes (boutons grisés aux bornes) ; pas : saut des boutons (la saisie reste libre).
// libelle : ce que représente le nombre, pour les boutons lus à voix haute (« Diminuer l'effectif »).
// Les autres attributs (id, required, placeholder…) vont sur le champ.
defineOptions({ inheritAttrs: false })
const props = defineProps({
  min: { type: Number, default: null },
  max: { type: Number, default: null },
  pas: { type: Number, default: 1 },
  libelle: { type: String, default: 'la valeur' },
})
const valeur = defineModel({ type: [Number, String], default: null }) // '' quand le champ est vidé

// Ramène dans les bornes
const borner = (n) => Math.min(props.max ?? Infinity, Math.max(props.min ?? -Infinity, n))

const vide = () => valeur.value === null || valeur.value === ''
const peutBaisser = () => !vide() && (props.min === null || valeur.value > props.min)
const peutMonter = () => props.max === null || vide() || valeur.value < props.max

const baisser = () => (valeur.value = borner(valeur.value - props.pas))
const monter = () => (valeur.value = vide() ? (props.min ?? 0) : borner(valeur.value + props.pas))
</script>

<template>
  <div class="stepper">
    <button
      type="button"
      :disabled="!peutBaisser()"
      :aria-label="`Diminuer ${libelle}`"
      @click="baisser"
    >
      <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10" /></svg>
    </button>
    <input v-model.number="valeur" v-bind="$attrs" type="number" :min="min" :max="max" />
    <button
      type="button"
      :disabled="!peutMonter()"
      :aria-label="`Augmenter ${libelle}`"
      @click="monter"
    >
      <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10M8 3v10" /></svg>
    </button>
  </div>
</template>
