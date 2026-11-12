<script setup>
// Champ facultatif : une case à cocher qui affiche ou masque d'autres champs (ex. « Ajouter un délai »).
// v-model : la case est cochée (true / false) ; libelle : le texte de la case ;
// le contenu placé entre les balises s'affiche seulement quand la case est cochée.
// <CaseOption v-model="form.avecDelai" libelle="Ajouter un délai"> <input type="date" … /> </CaseOption>
defineProps({ libelle: { type: String, required: true } })
const active = defineModel({ type: Boolean, default: false })
</script>

<template>
  <label class="choice choice-grande">
    <input v-model="active" type="checkbox" />
    {{ libelle }}
  </label>
  <slot v-if="active" />
</template>
