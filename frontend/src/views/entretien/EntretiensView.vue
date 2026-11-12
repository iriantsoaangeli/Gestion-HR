<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import EntretiensColonne from '../../components/entretien/EntretiensColonne.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/entretiens'
import { useChargement } from '../../utils/chargement'

// Liste des entretiens (DIRECTION, MANAGER, HR) : à gauche ceux en attente, à droite ceux finis (évalués).
// Données statiques pour l'instant (api/entretiens.js).
const enAttente = useChargement(api.enAttente, [])
const finis = useChargement(api.finis, [])
</script>

<template>
  <AppLayout title="Entretiens">
    <Erreur :message="enAttente.erreur.value || finis.erreur.value" />
    <div class="board">
      <EntretiensColonne
        titre="En attente"
        :entretiens="enAttente.donnees.value"
        :vide="enAttente.chargement.value ? 'Chargement…' : 'Aucun entretien en attente'"
        :masquer="['note', 'maNote']"
      />
      <EntretiensColonne
        titre="Finis"
        :entretiens="finis.donnees.value"
        :vide="finis.chargement.value ? 'Chargement…' : 'Aucun entretien fini'"
        :masquer="['maNote']"
        recents-dabord
      />
    </div>
  </AppLayout>
</template>
