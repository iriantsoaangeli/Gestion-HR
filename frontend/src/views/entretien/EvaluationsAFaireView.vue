<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import EntretiensColonne from '../../components/entretien/EntretiensColonne.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/entretiens'
import { useChargement } from '../../utils/chargement'

// Mes évaluations (DIRECTION, MANAGER, HR), comme « Demandes à traiter » :
// à gauche les entretiens dont je suis interviewer et que je n'ai pas encore évalués (une ligne ouvre
// directement le formulaire d'évaluation), à droite ceux que j'ai déjà évalués (une ligne ouvre
// la fiche de mon évaluation).
//
const aEvaluer = useChargement(api.aEvaluer, [])
const evalues = useChargement(api.evaluesParMoi, [])
</script>

<template>
  <AppLayout title="Évaluations à faire">
    <Erreur :message="aEvaluer.erreur.value || evalues.erreur.value" />
    <div class="board">
      <EntretiensColonne
        titre="À évaluer"
        :entretiens="aEvaluer.donnees.value"
        :vide="aEvaluer.chargement.value ? 'Chargement…' : 'Aucune évaluation à faire'"
        :masquer="['note', 'maNote']"
        :lien="(e) => `/entretiens/${e.id}/evaluation`"
      />
      <EntretiensColonne
        titre="Déjà évaluées par moi"
        :entretiens="evalues.donnees.value"
        :vide="evalues.chargement.value ? 'Chargement…' : 'Aucune évaluation faite'"
        :masquer="['note']"
        :lien="(e) => `/evaluations/${e.idEvaluation}`"
        recents-dabord
      />
    </div>
  </AppLayout>
</template>
