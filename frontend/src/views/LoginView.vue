<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Erreur from '../components/Erreur.vue'
import { seConnecter } from '../auth/session'

const router = useRouter()
const route = useRoute()
const identifiant = ref('')
const motDePasse = ref('')
const erreur = ref('')
const envoi = ref(false)

async function envoyer() {
  if (!identifiant.value || !motDePasse.value) {
    erreur.value = 'Veuillez remplir tous les champs.'
    return
  }
  erreur.value = ''
  envoi.value = true
  try {
    await seConnecter(identifiant.value, motDePasse.value)
    // Retour à la page demandée avant la redirection vers le login, sinon l'accueil
    router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (e) {
    erreur.value = e.message
    motDePasse.value = ''
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="center">
    <form class="card form" @submit.prevent="envoyer">
      <h1>Gestion RH</h1>

      <label for="identifiant">Identifiant</label>
      <input id="identifiant" v-model.trim="identifiant" autocomplete="username" autofocus />

      <label for="motDePasse">Mot de passe</label>
      <input id="motDePasse" v-model="motDePasse" type="password" autocomplete="current-password" />

      <Erreur :message="erreur" />
      <button type="submit" :disabled="envoi">{{ envoi ? 'Connexion…' : 'Se connecter' }}</button>
    </form>
  </div>
</template>
