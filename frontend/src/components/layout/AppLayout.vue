<script setup>
import { useRoute, useRouter } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Erreur from '../ui/Erreur.vue'

defineProps({ title: String })

// Page réservée demandée (router/index.js) : on explique pourquoi on est revenu à l'accueil
const route = useRoute()
const router = useRouter()
const fermer = () => router.replace({ query: {} })
</script>

<template>
  <div class="layout">
    <Sidebar />
    <main class="content">
      <h1 v-if="title">{{ title }}</h1>
      <Erreur
        v-if="route.query.refusee"
        :message="`La page ${route.query.refusee} est réservée à d'autres rôles : retour à votre page d'accueil.`"
        fermable
        @fermer="fermer"
      />
      <slot />
    </main>
  </div>
</template>
