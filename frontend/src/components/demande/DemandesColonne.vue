<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { labelPostes, totalPostes, texteStatut, classeStatut } from '../../utils/demande'

// Tableau de demandes (DemandeResponse de l'API) avec filtre (puces + recherche) et tri par en-tête.
// Les puces de filtre sont construites à partir des statuts présents dans la liste.
const props = defineProps({
  titre: { type: String, required: true },
  demandes: { type: Array, required: true },
  // Page ouverte par une ligne (par défaut la fiche de la demande)
  lien: { type: Function, default: (d) => `/demandes/${d.id}` },
  vide: { type: String, default: 'Pas de demande' },
})

// Sélection activée par l'usage : si le parent lie v-model:selection (id ou null), un clic sur une ligne
// la choisit (re-clic = désélection) au lieu d'ouvrir la fiche. Sans liaison, le tableau ne change pas.
const selection = defineModel('selection', { default: undefined })
const selectionnable = computed(() => selection.value !== undefined)

// Tri { cle, sens } : interne par défaut, pilotable depuis le parent avec v-model:tri
const tri = defineModel('tri', { default: () => ({ cle: 'date', sens: -1 }) }) // plus récentes d'abord

// Colonnes triables : valeur utilisée pour le tri
const COLONNES = [
  { cle: 'id', label: 'N°', valeur: (d) => d.id },
  { cle: 'date', label: 'Date', valeur: (d) => d.date },
  { cle: 'departement', label: 'Département', valeur: (d) => d.departement },
  { cle: 'postes', label: 'Postes', valeur: (d) => labelPostes(d) },
  { cle: 'nombre', label: 'Nb', valeur: (d) => totalPostes(d) },
  { cle: 'delai', label: 'Délai', valeur: (d) => d.delai || '9999' }, // sans délai = en dernier
  { cle: 'statut', label: 'Statut', valeur: (d) => texteStatut(d) },
]

const router = useRouter()
// Toute la ligne ouvre la page (le n° reste un vrai lien pour le clavier et le clic molette)
function ouvrir(d) {
  if (!selectionnable.value) return router.push(props.lien(d))
  selection.value = selection.value === d.id ? null : d.id
}

const filtre = ref('')
const recherche = ref('')

function trier(cle) {
  tri.value = tri.value.cle === cle ? { cle, sens: -tri.value.sens } : { cle, sens: 1 }
}

// Une puce par statut présent dans la liste
const filtres = computed(() => {
  const vus = new Map()
  props.demandes.forEach((d) => vus.set(d.statut, d.statutLibelle))
  return [...vus].map(([code, label]) => ({ code, label }))
})
const nombreParFiltre = (code) => props.demandes.filter((d) => !code || d.statut === code).length

const lignes = computed(() => {
  const texte = recherche.value.trim().toLowerCase()
  const colonne = COLONNES.find((c) => c.cle === tri.value.cle)
  return props.demandes
    .filter((d) => !filtre.value || d.statut === filtre.value)
    .filter((d) => !texte || [d.id, d.departement, labelPostes(d), d.motif].join(' ').toLowerCase().includes(texte))
    .sort((a, b) => {
      const va = colonne.valeur(a)
      const vb = colonne.valeur(b)
      return (typeof va === 'number' ? va - vb : String(va).localeCompare(String(vb), 'fr')) * tri.value.sens
    })
})

const ariaTri = (cle) => (tri.value.cle !== cle ? 'none' : tri.value.sens === 1 ? 'ascending' : 'descending')
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

    <p v-if="!lignes.length" class="vide">{{ vide }}</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th v-for="c in COLONNES" :key="c.cle" :aria-sort="ariaTri(c.cle)">
              <button type="button" class="tri" @click="trier(c.cle)">
                {{ c.label }}
                <span class="tri-fleche" aria-hidden="true">
                  {{ tri.cle !== c.cle ? '↕' : tri.sens === 1 ? '▲' : '▼' }}
                </span>
              </button>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="d in lignes"
            :key="d.id"
            class="ligne-cliquable"
            :class="{ selectionnee: selectionnable && d.id === selection }"
            :title="d.motif"
            :aria-selected="selectionnable ? d.id === selection : undefined"
            @click="ouvrir(d)"
          >
            <td><RouterLink :to="lien(d)" @click.stop>{{ d.id }}</RouterLink></td>
            <td>{{ d.date }}</td>
            <td>{{ d.departement }}</td>
            <td>{{ labelPostes(d) }}</td>
            <td class="nombre">{{ totalPostes(d) }}</td>
            <td>{{ d.delai || '—' }}</td>
            <td><span class="badge petit" :class="classeStatut(d)">{{ texteStatut(d) }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
