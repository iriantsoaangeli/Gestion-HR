<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import DemandesBoard from '../../components/demande/DemandesBoard.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'

// Toutes les demandes : ADMIN et DIRECTION (contrôlé par le backend et par le routeur)
const { donnees: demandes, erreur, chargement } = useChargement(api.toutes, [])
</script>

<template>
  <AppLayout title="Toutes les demandes">
    <Erreur :message="erreur" />
    <p v-if="chargement" class="help">Chargement…</p>
    <DemandesBoard v-else :demandes="demandes" />
  </AppLayout>
</template>
