<script setup>
import Sidebar from './Sidebar.vue'

defineProps({ title: String })
</script>

<template>
  <div class="layout">
    <Sidebar />
    <main class="content">
      <h1 v-if="title">{{ title }}</h1>
      <slot />
    </main>
  </div>
</template>
