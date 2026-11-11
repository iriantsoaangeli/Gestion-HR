<script setup>
import AppLayout from '../../components/layout/AppLayout.vue'
import DemandesColonne from '../../components/demande/DemandesColonne.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'

// À gauche : les demandes qui attendent ma décision ; à droite : celles sur lesquelles j'ai déjà décidé.
// La décision se prend dans la fiche (une ligne ouvre la fiche).
const aValider = useChargement(api.aTraiter, [])
const traitees = useChargement(api.traitees, [])
</script>

<template>
  <AppLayout title="Demandes à traiter">
    <Erreur :message="aValider.erreur.value || traitees.erreur.value" />
    <div class="board">
      <DemandesColonne
        titre="À valider"
        :demandes="aValider.donnees.value"
        :vide="aValider.chargement.value ? 'Chargement…' : 'Rien à valider'"
      />
      <DemandesColonne
        titre="Déjà traitées par moi"
        :demandes="traitees.donnees.value"
        :vide="traitees.chargement.value ? 'Chargement…' : 'Aucune demande traitée'"
      />
    </div>
  </AppLayout>
</template>
