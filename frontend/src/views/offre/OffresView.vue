<script setup>
import { computed } from 'vue'
import AppLayout from '../../components/layout/AppLayout.vue'
import OffresColonne from '../../components/offre/OffresColonne.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as apiOffres from '../../api/offres'
import { enAttente, finis } from '../../api/entretiens'
import { useChargement } from '../../utils/chargement'

// Liste des offres d'emploi (HR) : à gauche les ouvertes, à droite les closes. Une ligne ouvre la fiche.
const offres = useChargement(apiOffres.offres, [])
const entretiens = useChargement(
  () => Promise.all([enAttente(), finis()]).then((l) => l.flat()),
  [],
)

const chargement = computed(() => offres.chargement.value || entretiens.chargement.value)
const erreur = computed(() => offres.erreur.value || entretiens.erreur.value)

// Chaque offre (avec son nombre de candidats, renvoyé par l'API) et son nombre d'entretiens
const liste = computed(() =>
  offres.donnees.value.map((o) => ({
    ...o,
    entretiens: entretiens.donnees.value.filter((e) => e.idOffre === o.id).length,
  })),
)
const ouvertes = computed(() => liste.value.filter((o) => o.ouverte))
const closes = computed(() => liste.value.filter((o) => !o.ouverte))
</script>

<template>
  <AppLayout title="Offres d'emploi">
    <Erreur :message="erreur" />
    <div class="board">
      <OffresColonne
        titre="Ouvertes"
        :offres="ouvertes"
        :vide="chargement ? 'Chargement…' : 'Aucune offre ouverte'"
      />
      <OffresColonne
        titre="Closes"
        :offres="closes"
        :vide="chargement ? 'Chargement…' : 'Aucune offre close'"
      />
    </div>
  </AppLayout>
</template>
