<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Tableau from '../ui/Tableau.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import BarreRecherche from '../ui/BarreRecherche.vue'
import { contient } from '../../utils/texte'

// Carte « entretiens » : titre, filtre (puces de département + recherche) et tableau,
// sur le modèle de components/demande/DemandesColonne.vue (mêmes classes .colonne, .filtres, .puces).
// Données statiques pour l'instant (api/entretiens.js).
const props = defineProps({
  titre: { type: String, required: true },
  entretiens: { type: Array, required: true },
  vide: { type: String, default: 'Aucun entretien' },
  masquer: { type: Array, default: () => [] }, // clés des colonnes à ne pas afficher (ex. ['note'])
  recentsDabord: { type: Boolean, default: false }, // tri par date : plus récents d'abord (sinon plus proches)
  // Page ouverte par une ligne (par défaut la fiche de l'entretien)
  lien: { type: Function, default: (e) => `/entretiens/${e.id}` },
})

// « 18/11/2026 09:00 »
const dateHeure = (texte) =>
  new Date(texte).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

const COLONNES = [
  { cle: 'id', label: 'N°', classe: 'nombre' },
  { cle: 'date', label: 'Date', valeur: (e) => dateHeure(e.date), valeurTri: (e) => e.date },
  { cle: 'candidat', label: 'Candidat' },
  { cle: 'poste', label: 'Poste' }, // département en dessous (place gagnée ; les puces filtrent dessus)
  {
    cle: 'note',
    label: 'Note',
    classe: 'nombre',
    valeur: (e) => (e.note == null ? null : `${e.note}/100`),
    valeurTri: (e) => e.note,
  },
  {
    cle: 'maNote',
    label: 'Ma note',
    classe: 'nombre',
    valeur: (e) => (e.maNote == null ? null : `${e.maNote}/100`),
    valeurTri: (e) => e.maNote,
  },
]
const colonnes = computed(() => COLONNES.filter((c) => !props.masquer.includes(c.cle)))
// Toute la ligne ouvre la page (le n° reste un vrai lien pour le clavier et le clic molette)
const router = useRouter()
const ouvrir = (e) => router.push(props.lien(e))

const tri = ref({ cle: 'date', sens: props.recentsDabord ? -1 : 1 })

// Filtre au-dessus du tableau : département ('' = tous) + recherche (sans accents, utils/texte.js)
const filtre = reactive({ recherche: '', departement: '' })
const lignes = computed(() =>
  props.entretiens.filter(
    (e) =>
      (!filtre.departement || e.departement === filtre.departement) &&
      contient([e.id, e.candidat, e.poste, e.departement].join(' '), filtre.recherche),
  ),
)
</script>

<template>
  <section class="colonne">
    <h2>
      {{ titre }} <span class="count">{{ entretiens.length }}</span>
    </h2>

    <div class="filtres">
      <FiltrePuces
        v-model="filtre.departement"
        :elements="entretiens"
        :aria-label="`Département : ${titre}`"
      />
      <BarreRecherche
        v-model="filtre.recherche"
        placeholder="Rechercher (n°, candidat, poste)…"
        :aria-label="`Rechercher dans ${titre}`"
      />
    </div>

    <Tableau
      :lignes="lignes"
      :colonnes="colonnes"
      :vide="entretiens.length ? 'Aucun entretien pour ce filtre' : vide"
      :titre-ligne="(e) => e.remarques"
      v-model:tri="tri"
      cliquable
      @ouvrir="ouvrir"
    >
      <template #poste="{ ligne }">
        {{ ligne.poste }}
        <span class="cellule-detail">{{ ligne.departement }}</span>
      </template>
      <template #id="{ ligne }">
        <RouterLink :to="props.lien(ligne)" @click.stop>{{ ligne.id }}</RouterLink>
      </template>
    </Tableau>
  </section>
</template>
