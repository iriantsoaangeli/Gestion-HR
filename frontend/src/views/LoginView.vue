<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const identifiant = ref('')
const motDePasse = ref('')
const erreur = ref('')

// Pas encore d'authentification côté backend : on vérifie seulement que les champs sont remplis
function seConnecter() {
  if (!identifiant.value || !motDePasse.value) {
    erreur.value = 'Veuillez remplir tous les champs.'
    return
  }
  router.push('/')
}
</script>

<template>
  <div class="center">
    <form class="card form" @submit.prevent="seConnecter">
      <h1>Gestion RH</h1>

      <label for="identifiant">Identifiant</label>
      <input id="identifiant" v-model.trim="identifiant" autocomplete="username" />

      <label for="motDePasse">Mot de passe</label>
      <input id="motDePasse" v-model="motDePasse" type="password" autocomplete="current-password" />

      <p v-if="erreur" class="error">{{ erreur }}</p>
      <button type="submit">Se connecter</button>
    </form>
  </div>
</template>
