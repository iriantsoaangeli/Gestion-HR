<script setup>
import { ETAPES } from '../data/etapes'

// Les entrées sans route ne sont pas encore cliquables
const items = [
  { label: 'Accueil', to: '/' },
  { label: 'Postes' },
  { label: 'Départements' },
  { label: 'Personnes' },
  { label: 'Emplois' },
  ]
</script>

<template>
  <aside class="sidebar">
    <div class="brand">Gestion RH</div>
    <nav>
      <ul>
        <li v-for="item in items" :key="item.label">
          <RouterLink v-if="item.to" :to="item.to">{{ item.label }}</RouterLink>
          <a v-else href="#" @click.prevent>{{ item.label }}</a>
        </li>
      </ul>

      <p class="section">Demandes</p>
      <ul>
        <li><RouterLink to="/mes-demandes">Mes demandes</RouterLink></li>
        <li v-for="e in ETAPES" :key="e.code">
          <RouterLink :to="`/demande/${e.code}`">{{ e.label }}</RouterLink>
        </li>
      </ul>

      <!-- TEMPORAIRE : liens vers les pages de démonstration des erreurs — à supprimer -->
      <p class="section">Temporaire</p>
      <ul>
        <li><RouterLink to="/temporaire/erreur-login">Erreurs login</RouterLink></li>
        <li><RouterLink to="/temporaire/erreur-demande">Erreurs demande</RouterLink></li>
        <li><RouterLink to="/temporaire/erreur-validation">Erreurs validation</RouterLink></li>
      </ul>
    </nav>
  </aside>
</template>
