<script setup>
import { computed, ref } from 'vue'
import { contient, normaliser } from '../../utils/texte'

// Liste déroulante avec recherche : les suggestions se filtrent pendant la saisie.
// options : [{ id, label }] — creation : propose « Créer "texte" » si aucun libellé identique
const props = defineProps({
  id: { type: String, required: true },
  options: { type: Array, required: true },
  placeholder: { type: String, default: 'Rechercher…' },
  creation: { type: Boolean, default: false },
  vide: { type: String, default: 'Aucun résultat.' },
})
// choisir : l'option choisie, ou { label } pour une création
const emit = defineEmits(['choisir'])

const texte = ref('')
const ouvert = ref(false)
const actif = ref(0)

const suggestions = computed(() => {
  // Insensible à la casse et aux accents (utils/texte.js)
  const t = texte.value.trim()
  const liste = props.options.filter((o) => contient(o.label, t))
  const existe = props.options.some((o) => normaliser(o.label) === normaliser(t))
  if (props.creation && t && !existe) liste.push({ label: texte.value.trim(), nouveau: true })
  return liste
})

function ouvrir() {
  ouvert.value = true
  actif.value = 0
}

function choisir(option) {
  emit('choisir', option.nouveau ? { label: option.label } : option)
  texte.value = ''
  actif.value = 0
}

function deplacer(pas) {
  if (!ouvert.value) return ouvrir()
  const n = suggestions.value.length
  if (n) actif.value = (actif.value + pas + n) % n
}

function valider() {
  const option = suggestions.value[actif.value]
  if (ouvert.value && option) choisir(option)
}
</script>

<template>
  <div class="recherche-select">
    <input
      :id="id"
      v-model="texte"
      type="search"
      autocomplete="off"
      role="combobox"
      :aria-expanded="ouvert"
      :aria-controls="`${id}-liste`"
      :placeholder="placeholder"
      @focus="ouvrir"
      @input="ouvrir"
      @blur="ouvert = false"
      @keydown.down.prevent="deplacer(1)"
      @keydown.up.prevent="deplacer(-1)"
      @keydown.enter.prevent="valider"
      @keydown.esc="ouvert = false"
    />
    <ul v-if="ouvert" :id="`${id}-liste`" class="recherche-liste" role="listbox">
      <!-- mousedown.prevent : garde le focus dans le champ, le blur ne ferme pas la liste avant le clic -->
      <li
        v-for="(o, i) in suggestions"
        :key="o.nouveau ? 'nouveau' : o.id"
        role="option"
        :aria-selected="i === actif"
        :class="{ actif: i === actif }"
        @mousedown.prevent="choisir(o)"
        @mouseenter="actif = i"
      >
        <template v-if="o.nouveau">Créer « {{ o.label }} »</template>
        <template v-else>{{ o.label }}</template>
      </li>
      <li v-if="!suggestions.length" class="help">{{ vide }}</li>
    </ul>
  </div>
</template>
