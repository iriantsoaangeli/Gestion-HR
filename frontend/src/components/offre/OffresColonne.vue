<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Tableau from '../ui/Tableau.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import BarreRecherche from '../ui/BarreRecherche.vue'
import { contient } from '../../utils/texte'

// Carte « offres » : titre, filtre (puces de département + recherche) et tableau, sur le modèle de
// components/entretien/EntretiensColonne.vue et components/demande/DemandesColonne.vue (mêmes classes).
// offres : offres de api/offres.js, avec en plus « candidats » et « entretiens » (nombres, calculés par la vue).
const props = defineProps({
  titre: { type: String, required: true },
  offres: { type: Array, required: true },
  vide: { type: String, default: 'Aucune offre' },
})

const COLONNES = [
  { cle: 'id', label: 'N°', classe: 'nombre' },
  // Département et responsable sous le poste : la place manque pour deux colonnes de plus
  { cle: 'poste', label: 'Poste' },
  { cle: 'delai', label: 'Limite', classe: 'sans-coupure', valeurTri: (o) => o.delai || '9999' }, // sans date = en dernier
  { cle: 'candidats', label: 'Candidats', classe: 'nombre' },
  { cle: 'entretiens', label: 'Entretiens', classe: 'nombre' },
]
const tri = ref({ cle: 'delai', sens: 1 }) // date limite la plus proche d'abord

// Toute la ligne ouvre la fiche (le n° reste un vrai lien pour le clavier et le clic molette)
const lien = (o) => `/offres/${o.id}`
const router = useRouter()
const ouvrir = (o) => router.push(lien(o))

// Filtre au-dessus du tableau : département ('' = tous) + recherche (sans accents, utils/texte.js)
const filtre = reactive({ recherche: '', departement: '' })
const lignes = computed(() =>
  props.offres.filter(
    (o) =>
      (!filtre.departement || o.departement === filtre.departement) &&
      contient(
        [o.id, o.poste, o.departement, o.responsable, o.description].join(' '),
        filtre.recherche,
      ),
  ),
)
</script>

<template>
  <section class="colonne">
    <h2>
      {{ titre }} <span class="count">{{ offres.length }}</span>
    </h2>

    <div class="filtres">
      <FiltrePuces
        v-model="filtre.departement"
        :elements="offres"
        :aria-label="`Département : ${titre}`"
      />
      <BarreRecherche
        v-model="filtre.recherche"
        placeholder="Rechercher (n°, poste, responsable)…"
        :aria-label="`Rechercher dans ${titre}`"
      />
    </div>

    <Tableau
      v-model:tri="tri"
      :lignes="lignes"
      :colonnes="COLONNES"
      :vide="offres.length ? 'Aucune offre pour ce filtre' : vide"
      :titre-ligne="(o) => o.description"
      cliquable
      @ouvrir="ouvrir"
    >
      <template #id="{ ligne }">
        <RouterLink :to="lien(ligne)" @click.stop>{{ ligne.id }}</RouterLink>
      </template>
      <template #poste="{ ligne }">
        {{ ligne.poste }}
        <span class="cellule-detail">{{ ligne.departement }} · {{ ligne.responsable }}</span>
      </template>
    </Tableau>
  </section>
</template>
