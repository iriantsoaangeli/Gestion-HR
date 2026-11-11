<script setup>
// Les entrées sans route ne sont pas encore cliquables
const items = [
  { label: 'Accueil', to: '/' },
  { label: 'Postes' },
  { label: 'Départements' },
  { label: 'Personnes' },
  { label: 'Emplois' },
  { label: 'Demandes', to: '/demande' },
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
    </nav>
  </aside>
</template>
