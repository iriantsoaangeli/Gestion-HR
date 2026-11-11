<script setup>
import { computed } from 'vue'
import DemandesColonne from './DemandesColonne.vue'
import { ETAPES } from '../../data/etapes'

// Deux tableaux côte à côte : demandes en cours à gauche, déjà traitées à droite.
// Utilisé par Mes demandes, Toutes les demandes et Demandes à traiter.
// titres : libellés des deux tableaux ; lienEnCours : page ouverte par une demande en cours
const props = defineProps({
  demandes: { type: Array, required: true },
  titreEnCours: { type: String, default: 'En cours' },
  titreTraitees: { type: String, default: 'Déjà traitées' },
  lienEnCours: { type: Function, default: undefined },
})

// Manager crée la demande : les attentes commencent à HR
const FILTRES_EN_COURS = ETAPES.slice(1).map((e) => ({ code: e.code, label: e.label }))
const FILTRES_TRAITEES = [
  { code: 'approuvee', label: 'Terminées' },
  { code: 'refusee', label: 'Refusées' },
]
const TRAITEES = ['approuvee', 'refusee']

const enCours = computed(() => props.demandes.filter((d) => !TRAITEES.includes(d.etape)))
const traitees = computed(() => props.demandes.filter((d) => TRAITEES.includes(d.etape)))
</script>

<template>
  <div class="board">
    <DemandesColonne :titre="titreEnCours" :demandes="enCours" :filtres="FILTRES_EN_COURS" :lien="lienEnCours" />
    <DemandesColonne :titre="titreTraitees" :demandes="traitees" :filtres="FILTRES_TRAITEES" />
  </div>
</template>
