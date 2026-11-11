<script setup>
import { computed } from 'vue'

// Message d'erreur commun à toute l'application.
// message : un texte, ou une liste de textes (affichée en puces). Rien n'est affiché s'il est vide.
// titre : ligne en gras au-dessus du message (facultatif)
// fermable : affiche un ✕ qui émet « fermer » (le parent vide alors le message)
const props = defineProps({
  message: { type: [String, Array], default: '' },
  titre: { type: String, default: '' },
  fermable: { type: Boolean, default: false },
})
defineEmits(['fermer'])

const messages = computed(() =>
  (Array.isArray(props.message) ? props.message : [props.message]).filter(Boolean),
)
</script>

<template>
  <div v-if="messages.length || titre" class="erreur" role="alert">
    <svg class="erreur-icone" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="10" />
      <path d="M12 7v6M12 16.5v.5" />
    </svg>

    <div class="erreur-texte">
      <strong v-if="titre">{{ titre }}</strong>
      <ul v-if="messages.length > 1">
        <li v-for="m in messages" :key="m">{{ m }}</li>
      </ul>
      <p v-else-if="messages.length">{{ messages[0] }}</p>
      <slot />
    </div>

    <button v-if="fermable" type="button" class="erreur-fermer" aria-label="Fermer" @click="$emit('fermer')">
      ✕
    </button>
  </div>
</template>
