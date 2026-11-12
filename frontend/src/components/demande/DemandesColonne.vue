<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import Tableau from '../ui/Tableau.vue'
import { labelPostes, totalPostes, texteStatut, classeStatut } from '../../utils/demande'
import { contient } from '../../utils/texte'

// Carte « demandes » (DemandeResponse de l'API) : titre, filtre (puces + recherche) et tableau.
// Le tri, le défilement et la sélection viennent du tableau générique (components/ui/Tableau.vue).
// Les puces de filtre sont construites à partir des statuts présents dans la liste.
const props = defineProps({
  titre: { type: String, required: true },
  demandes: { type: Array, required: true },
  // Page ouverte par une ligne (par défaut la fiche de la demande)
  lien: { type: Function, default: (d) => `/demandes/${d.id}` },
  vide: { type: String, default: 'Pas de demande' },
  masquer: { type: Array, default: () => [] }, // clés des colonnes à ne pas afficher (ex. ['statut'])
})

// Transmis au tableau : sélection (v-model:selection, activée seulement si le parent la lie) et tri
const selection = defineModel('selection', { default: undefined })
const tri = defineModel('tri', { default: () => ({ cle: 'date', sens: -1 }) }) // plus récentes d'abord

const COLONNES = [
  { cle: 'id', label: 'N°' },
  { cle: 'date', label: 'Date' },
  { cle: 'departement', label: 'Département' },
  { cle: 'postes', label: 'Postes', valeur: labelPostes },
  { cle: 'nombre', label: 'Nb', valeur: totalPostes, classe: 'nombre' },
  { cle: 'delai', label: 'Délai', valeurTri: (d) => d.delai || '9999' }, // sans délai = en dernier
  { cle: 'statut', label: 'Statut', valeur: texteStatut },
]

const colonnes = computed(() => COLONNES.filter((c) => !props.masquer.includes(c.cle)))

const router = useRouter()
// Toute la ligne ouvre la page (le n° reste un vrai lien pour le clavier et le clic molette)
const ouvrir = (d) => router.push(props.lien(d))

const filtre = ref('')
const recherche = ref('')

// Une puce par statut présent dans la liste
const filtres = computed(() => {
  const vus = new Map()
  props.demandes.forEach((d) => vus.set(d.statut, d.statutLibelle))
  return [...vus].map(([code, label]) => ({ code, label }))
})
const nombreParFiltre = (code) => props.demandes.filter((d) => !code || d.statut === code).length

// Recherche insensible à la casse et aux accents (utils/texte.js)
const lignes = computed(() =>
  props.demandes
    .filter((d) => !filtre.value || d.statut === filtre.value)
    .filter((d) => contient([d.id, d.departement, labelPostes(d), d.motif].join(' '), recherche.value)),
)
</script>

<template>
  <section class="colonne">
    <h2>{{ titre }} <span class="count">{{ demandes.length }}</span></h2>

    <div class="filtres">
      <div v-if="filtres.length > 1" class="puces" role="group" :aria-label="`Filtrer : ${titre}`">
        <button
          v-for="f in [{ code: '', label: 'Toutes' }, ...filtres]"
          :key="f.code"
          type="button"
          class="puce"
          :class="{ active: filtre === f.code }"
          :aria-pressed="filtre === f.code"
          @click="filtre = f.code"
        >
          {{ f.label }} <span class="count">{{ nombreParFiltre(f.code) }}</span>
        </button>
      </div>
      <input
        v-model="recherche"
        type="search"
        class="filtre-recherche"
        placeholder="Rechercher (n°, département, poste, motif)…"
        :aria-label="`Rechercher dans ${titre}`"
      />
    </div>

    <Tableau
      v-model:selection="selection"
      v-model:tri="tri"
      :lignes="lignes"
      :colonnes="colonnes"
      :vide="vide"
      :titre-ligne="(d) => d.motif"
      cliquable
      @ouvrir="ouvrir"
    >
      <template #id="{ ligne }">
        <RouterLink :to="lien(ligne)" @click.stop>{{ ligne.id }}</RouterLink>
      </template>
      <template #statut="{ ligne }">
        <span class="badge petit" :class="classeStatut(ligne)">{{ texteStatut(ligne) }}</span>
      </template>
    </Tableau>
  </section>
</template>
