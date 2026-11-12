<script setup>
import { computed } from 'vue'

// Tableau réutilisable : tri par en-tête, défilement avec en-têtes fixes, sélection facultative.
// lignes : une liste renvoyée par l'API telle quelle ([XxxResponse], JSON camelCase, identifiant `id`).
// colonnes : [{ cle, label, valeur?, valeurTri?, classe?, triable? }]
//   valeur(ligne) : ce qui est affiché (par défaut ligne[cle]) et trié ; nombre = tri numérique, sinon texte
//   valeurTri(ligne) : valeur de tri si elle diffère de l'affichage (ex. date vide triée en dernier)
//   classe : classe des cellules (ex. 'nombre' pour aligner à droite) ; triable : false pour ne pas trier
// Contenu d'une cellule personnalisé avec le slot #<cle>="{ ligne, valeur }" (lien, badge…).
const props = defineProps({
  lignes: { type: Array, required: true },
  colonnes: { type: Array, required: true },
  cleLigne: { type: String, default: 'id' }, // champ unique d'une ligne (clé Vue et sélection)
  cliquable: { type: Boolean, default: false }, // un clic sur une ligne émet « ouvrir »
  titreLigne: { type: Function, default: null }, // infobulle d'une ligne
  vide: { type: String, default: 'Aucune donnée' },
})
const emit = defineEmits(['ouvrir'])

// Sélection activée par l'usage : si le parent lie v-model:selection (valeur de cleLigne, ou null),
// un clic choisit la ligne (re-clic = désélection) au lieu d'émettre « ouvrir ».
const selection = defineModel('selection', { default: undefined })
const selectionnable = computed(() => selection.value !== undefined)

// Tri { cle, sens: 1 | -1 } : interne par défaut, pilotable depuis le parent avec v-model:tri.
// Sans tri initial, l'ordre reçu de l'API est gardé.
const tri = defineModel('tri', { default: () => ({ cle: null, sens: 1 }) })

const valeur = (colonne, ligne) => (colonne.valeur ? colonne.valeur(ligne) : ligne[colonne.cle])
const valeurTri = (colonne, ligne) => (colonne.valeurTri ? colonne.valeurTri(ligne) : valeur(colonne, ligne))

function trier(colonne) {
  if (colonne.triable === false) return
  const cle = colonne.cle
  tri.value = tri.value.cle === cle ? { cle, sens: -tri.value.sens } : { cle, sens: 1 }
}

const lignesTriees = computed(() => {
  const colonne = props.colonnes.find((c) => c.cle === tri.value.cle)
  if (!colonne) return props.lignes
  return [...props.lignes].sort((a, b) => {
    const va = valeurTri(colonne, a) ?? ''
    const vb = valeurTri(colonne, b) ?? ''
    return (typeof va === 'number' && typeof vb === 'number' ? va - vb : String(va).localeCompare(String(vb), 'fr')) * tri.value.sens
  })
})

const estChoisie = (ligne) => selectionnable.value && ligne[props.cleLigne] === selection.value

function cliquer(ligne) {
  if (!selectionnable.value) return props.cliquable && emit('ouvrir', ligne)
  const id = ligne[props.cleLigne]
  selection.value = selection.value === id ? null : id
}

const ariaTri = (cle) => (tri.value.cle !== cle ? 'none' : tri.value.sens === 1 ? 'ascending' : 'descending')
</script>

<template>
  <div class="table-wrap tableau">
    <p v-if="!lignes.length" class="tableau-vide">{{ vide }}</p>

    <table v-else class="table">
      <thead>
        <tr>
          <th v-for="c in colonnes" :key="c.cle" :class="c.classe" :aria-sort="c.triable === false ? undefined : ariaTri(c.cle)">
            <span v-if="c.triable === false">{{ c.label }}</span>
            <button v-else type="button" class="tri" @click="trier(c)">
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
          v-for="l in lignesTriees"
          :key="l[cleLigne]"
          :class="{ 'ligne-cliquable': cliquable || selectionnable, selectionnee: estChoisie(l) }"
          :title="titreLigne?.(l)"
          :aria-selected="selectionnable ? estChoisie(l) : undefined"
          @click="cliquer(l)"
        >
          <td v-for="c in colonnes" :key="c.cle" :class="c.classe">
            <slot :name="c.cle" :ligne="l" :valeur="valeur(c, l)">{{ valeur(c, l) ?? '—' }}</slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
