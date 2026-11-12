<script setup>
import { useRouter } from 'vue-router'
import { utilisateur, seDeconnecter } from '../../auth/session'

const router = useRouter()

async function deconnexion() {
  await seDeconnecter()
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">Gestion RH</div>
    <nav>
      <p class="section">Demandes</p>
      <ul>
        <li v-if="utilisateur?.peutCreerDemande"><RouterLink to="/demande">Nouvelle demande</RouterLink></li>
        <!-- Rôle MANAGER : le seul qui crée des demandes, donc le seul qui a « ses » demandes -->
        <li v-if="utilisateur?.peutCreerDemande"><RouterLink to="/mes-demandes">Mes demandes</RouterLink></li>
        <li v-if="utilisateur?.valideDesDemandes">
          <RouterLink to="/demandes-a-traiter">Demandes à traiter</RouterLink>
        </li>
        <li v-if="utilisateur?.voitToutesLesDemandes">
          <RouterLink to="/toutes-les-demandes">Toutes les demandes</RouterLink>
        </li>
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
