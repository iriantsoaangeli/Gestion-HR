<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Tableau from '../ui/Tableau.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import BarreRecherche from '../ui/BarreRecherche.vue'
import { labelPostes, totalPostes, texteStatut, classeStatut } from '../../utils/demande'
import { contient } from '../../utils/texte'
import { jour } from '../../utils/dates'

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
  // false : pas de puces de statut (ex. demandes toutes approuvées ; les filtres sont alors au-dessus)
  pucesStatut: { type: Boolean, default: true },
})

// Transmis au tableau : sélection (v-model:selection, activée seulement si le parent la lie) et tri
const selection = defineModel('selection', { default: undefined })
const tri = defineModel('tri', { default: () => ({ cle: 'date', sens: -1 }) }) // plus récentes d'abord

const COLONNES = [
  { cle: 'id', label: 'N°' },
  { cle: 'date', label: 'Date', valeur: (d) => jour(d.date), valeurTri: (d) => d.date }, // tri à l'heure près
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

// Délai : deux cases cochées au départ (tout s'affiche) ; décocher retire les demandes avec / sans délai
const delai = reactive({ avec: true, sans: true })
const avecDelai = computed(() => props.demandes.filter((d) => d.delai).length)
const gardeDelai = (d) => (d.delai ? delai.avec : delai.sans)

// Recherche insensible à la casse et aux accents (utils/texte.js)
const lignes = computed(() =>
  props.demandes
    .filter((d) => !filtre.value || d.statut === filtre.value)
    .filter(gardeDelai)
    .filter((d) =>
      contient([d.id, d.departement, labelPostes(d), d.motif].join(' '), recherche.value),
    ),
)
</script>

<template>
  <section class="colonne">
    <h2>
      {{ titre }} <span class="count">{{ demandes.length }}</span>
    </h2>

    <div class="filtres">
      <FiltrePuces
        v-if="pucesStatut"
        v-model="filtre"
        :elements="demandes"
        :code="(d) => d.statut"
        :libelle="(d) => d.statutLibelle"
        :ordre-alphabetique="false"
        tous="Toutes"
        :aria-label="`Filtrer : ${titre}`"
      />
      <div class="filtre-cases" role="group" aria-label="Délai">
        <label class="choice">
          <input v-model="delai.avec" type="checkbox" />
          Avec délai <span class="count">{{ avecDelai }}</span>
        </label>
        <label class="choice">
          <input v-model="delai.sans" type="checkbox" />
          Sans délai <span class="count">{{ demandes.length - avecDelai }}</span>
        </label>
      </div>
      <BarreRecherche
        v-model="recherche"
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
