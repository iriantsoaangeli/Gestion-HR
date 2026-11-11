<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { labelDepartement, labelPostes, totalPostes } from '../../data/demandes'
import { labelEtape } from '../../data/etapes'

// Tableau de demandes avec filtre (puces + recherche) et tri en cliquant sur les en-têtes.
// filtres : [{ code, label }] — code = valeur de d.etape ('' = tout). Le n° ouvre la fiche.
const props = defineProps({
  titre: { type: String, required: true },
  demandes: { type: Array, required: true },
  filtres: { type: Array, default: () => [] },
})

// Statut lisible et classe du badge
function statut(d) {
  if (d.etape === 'approuvee') return { texte: 'Approuvée', classe: 'ok' }
  if (d.etape === 'refusee') return { texte: `Refusée par ${d.refuseePar}`, classe: 'ko' }
  return { texte: `En attente ${labelEtape(d.etape)}`, classe: 'attente' }
}

// Colonnes triables : valeur utilisée pour le tri
const COLONNES = [
  { cle: 'id', label: 'N°', valeur: (d) => d.id },
  { cle: 'date', label: 'Date', valeur: (d) => d.date },
  { cle: 'departement', label: 'Département', valeur: (d) => labelDepartement(d) },
  { cle: 'postes', label: 'Postes', valeur: (d) => labelPostes(d) },
  { cle: 'nombre', label: 'Nb', valeur: (d) => totalPostes(d) },
  { cle: 'delai', label: 'Délai', valeur: (d) => d.delai || '9999' }, // sans délai = en dernier
  { cle: 'statut', label: 'Statut', valeur: (d) => statut(d).texte },
]

const router = useRouter()
// Toute la ligne ouvre la fiche (le n° reste un vrai lien pour le clavier et le clic molette)
const ouvrir = (d) => router.push(`/demandes/${d.id}`)

const filtre = ref('')
const recherche = ref('')
const tri = ref({ cle: 'date', sens: -1 }) // plus récentes d'abord

function trier(cle) {
  tri.value = tri.value.cle === cle ? { cle, sens: -tri.value.sens } : { cle, sens: 1 }
}

const nombreParFiltre = (code) => props.demandes.filter((d) => !code || d.etape === code).length

const lignes = computed(() => {
  const texte = recherche.value.trim().toLowerCase()
  const colonne = COLONNES.find((c) => c.cle === tri.value.cle)
  return props.demandes
    .filter((d) => !filtre.value || d.etape === filtre.value)
    .filter((d) =>
      !texte ||
      [d.id, labelDepartement(d), labelPostes(d), d.motif].join(' ').toLowerCase().includes(texte),
    )
    .sort((a, b) => {
      const va = colonne.valeur(a)
      const vb = colonne.valeur(b)
      return (typeof va === 'number' ? va - vb : String(va).localeCompare(String(vb), 'fr')) * tri.value.sens
    })
})

const ariaTri = (cle) =>
  tri.value.cle !== cle ? 'none' : tri.value.sens === 1 ? 'ascending' : 'descending'
</script>

<template>
  <section class="colonne">
    <h2>{{ titre }} <span class="count">{{ demandes.length }}</span></h2>

    <div class="filtres">
      <div v-if="filtres.length" class="puces" role="group" :aria-label="`Filtrer : ${titre}`">
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

    <p v-if="!lignes.length" class="vide">Pas de demande</p>

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
          <tr v-for="d in lignes" :key="d.id" class="ligne-cliquable" :title="d.motif" @click="ouvrir(d)">
            <td><RouterLink :to="`/demandes/${d.id}`" @click.stop>{{ d.id }}</RouterLink></td>
            <td>{{ d.date }}</td>
            <td>{{ labelDepartement(d) }}</td>
            <td>{{ labelPostes(d) }}</td>
            <td class="nombre">{{ totalPostes(d) }}</td>
            <td>{{ d.delai || '—' }}</td>
            <td><span class="badge petit" :class="statut(d).classe">{{ statut(d).texte }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
