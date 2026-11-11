<script setup>
import { useRouter } from 'vue-router'
import { ETAPES } from '../data/etapes'
import { utilisateur, seDeconnecter } from '../auth/session'

const router = useRouter()

async function deconnexion() {
  await seDeconnecter()
  router.push('/login')
}

// Manager crée la demande, les autres étapes valident
const ETAPES_VALIDATION = ETAPES.slice(1)

// Les entrées sans route ne sont pas encore cliquables
const items = [
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
        <li><RouterLink to="/demande">Nouvelle demande</RouterLink></li>
        <li><RouterLink to="/mes-demandes">Mes demandes</RouterLink></li>
        <li v-if="utilisateur?.voitToutesLesDemandes">
          <RouterLink to="/toutes-les-demandes">Toutes les demandes</RouterLink>
        </li>
      </ul>

      <!-- Une page par étape : chaque étape a ses propres utilisateurs -->
      <p class="section">Validation</p>
      <ul>
        <li v-for="e in ETAPES_VALIDATION" :key="e.code">
          <RouterLink :to="`/validation/${e.code}`">{{ e.label }}</RouterLink>
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

    <!-- Utilisateur connecté -->
    <div v-if="utilisateur" class="compte">
      <div>
        <strong>{{ utilisateur.login }}</strong>
        <span class="help">{{ utilisateur.roleLibelle }}</span>
      </div>
      <button type="button" class="bouton-deconnexion" @click="deconnexion">Se déconnecter</button>
    </div>
  </aside>
</template>
