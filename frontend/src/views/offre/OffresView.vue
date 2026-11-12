<script setup>
import { computed } from 'vue'
import AppLayout from '../../components/layout/AppLayout.vue'
import OffresColonne from '../../components/offre/OffresColonne.vue'
import Erreur from '../../components/ui/Erreur.vue'
import * as apiOffres from '../../api/offres'
import { dossiers as chargerDossiers } from '../../api/dossiers'
import { enAttente, finis } from '../../api/entretiens'
import { useChargement } from '../../utils/chargement'

// Liste des offres d'emploi (HR) : à gauche les ouvertes, à droite les closes. Une ligne ouvre la fiche.
// Données statiques pour l'instant (api/offres.js, api/dossiers.js, api/entretiens.js).
const offres = useChargement(apiOffres.offres, [])
const dossiers = useChargement(chargerDossiers, [])
const entretiens = useChargement(
  () => Promise.all([enAttente(), finis()]).then((l) => l.flat()),
  [],
)

const chargement = computed(
  () => offres.chargement.value || dossiers.chargement.value || entretiens.chargement.value,
)
const erreur = computed(
  () => offres.erreur.value || dossiers.erreur.value || entretiens.erreur.value,
)

// Chaque offre avec son nombre de candidats (dossiers) et d'entretiens
const liste = computed(() =>
  offres.donnees.value.map((o) => ({
    ...o,
    candidats: dossiers.donnees.value.filter((d) => d.idOffre === o.id).length,
    entretiens: entretiens.donnees.value.filter((e) => e.idOffre === o.id).length,
  })),
)
const ouvertes = computed(() => liste.value.filter((o) => o.status === 'OUVERT'))
const closes = computed(() => liste.value.filter((o) => o.status === 'CLOS'))
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
