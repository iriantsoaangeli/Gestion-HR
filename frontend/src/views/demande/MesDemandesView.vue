<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import DemandesBoard from '../../components/demande/DemandesBoard.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'

// Demandes créées par l'utilisateur connecté (c'est aussi l'accueil)
const { donnees: demandes, erreur, chargement } = useChargement(api.mesDemandes, [])
</script>

<template>
  <AppLayout title="Mes demandes">
    <Erreur :message="erreur" />
    <p v-if="chargement" class="help">Chargement…</p>
    <DemandesBoard v-else :demandes="demandes" />
  </AppLayout>
</template>
