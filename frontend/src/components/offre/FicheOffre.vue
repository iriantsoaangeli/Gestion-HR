<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Tableau from '../ui/Tableau.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import BarreRecherche from '../ui/BarreRecherche.vue'
import { contient } from '../../utils/texte'
import { dateLongue, joursAvant } from '../../utils/dates'

// Fiche d'une offre d'emploi : en-tête (sur le modèle de components/demande/FicheDemande.vue),
// informations, description, puis deux tableaux : candidats (dossiers) et entretiens de l'offre.
// demande : demande de personnel d'origine (null si inconnue).
const props = defineProps({
  offre: { type: Object, required: true },
  dossiers: { type: Array, required: true },
  entretiens: { type: Array, required: true },
  demande: { type: Object, default: null },
})
const o = computed(() => props.offre)
const ouverte = computed(() => o.value.status === 'OUVERT')

const router = useRouter()
// Formulaire de préparation d'un entretien, pour l'offre (et un candidat si donné)
const preparer = (dossier) =>
  router.push({
    path: '/entretiens/nouveau',
    query: dossier ? { offre: o.value.id, dossier: dossier.id } : { offre: o.value.id },
  })
const lienEntretien = (e) => `/entretiens/${e.id}`

// « 18/11/2026 » ou « 18/11/2026 09:00 »
const dateCourte = (texte, heure = false) =>
  new Date(texte).toLocaleString(
    'fr-FR',
    heure ? { dateStyle: 'short', timeStyle: 'short' } : { dateStyle: 'short' },
  )

// Date limite : texte + urgence (moins de 2 semaines = proche), seulement si l'offre est ouverte
const delai = computed(() => {
  const j = joursAvant(o.value.delai)
  if (j === null || !ouverte.value) return null
  if (j < 0) return { texte: `Dépassée de ${-j} jour(s)`, classe: 'ko' }
  return { texte: `${j} jour(s) restant(s)`, classe: j < 14 ? 'attente' : 'ok' }
})

const classeEntretien = (e) => (e.statut === 'FINI' ? 'ok' : 'attente')

// --- Candidats : un dossier = un candidat, avec ses entretiens (un candidat peut en avoir plusieurs) ---
const candidats = computed(() =>
  props.dossiers.map((d) => ({
    ...d,
    entretiens: props.entretiens
      .filter((e) => e.idDossier === d.id)
      .sort((a, b) => a.date.localeCompare(b.date)),
  })),
)
const COLONNES_CANDIDATS = [
  { cle: 'candidat', label: 'Candidat' }, // employé(e) ou externe en dessous
  { cle: 'date', label: 'Dépôt', valeur: (d) => dateCourte(d.date), valeurTri: (d) => d.date },
  { cle: 'fichiers', label: 'Pièces jointes', valeur: (d) => d.fichiers.join(', ') },
  { cle: 'entretiens', label: 'Entretiens', valeurTri: (d) => d.entretiens.at(-1)?.date ?? '' },
]
const triCandidats = ref({ cle: 'date', sens: 1 })
// Puces : avec / sans entretien (tous les candidats sont du même département, celui de l'offre)
const PUCES_CANDIDATS = [
  { code: 'sans', label: 'Sans entretien' },
  { code: 'avec', label: 'Avec entretien' },
]
const filtreCandidats = reactive({ recherche: '', puce: '' })
const garde = (d, code) => !code || (code === 'avec') === d.entretiens.length > 0
const lignesCandidats = computed(() =>
  candidats.value.filter(
    (d) =>
      garde(d, filtreCandidats.puce) &&
      contient([d.candidat, d.email, d.tel, ...d.fichiers].join(' '), filtreCandidats.recherche),
  ),
)

// --- Entretiens de l'offre ---
const interviewers = (e) => e.interviewers.map((i) => i.detenteur).join(', ')
const COLONNES_ENTRETIENS = [
  { cle: 'id', label: 'N°', classe: 'nombre' },
  { cle: 'date', label: 'Date', valeur: (e) => dateCourte(e.date, true), valeurTri: (e) => e.date },
  { cle: 'candidat', label: 'Candidat' }, // interviewers en dessous
  { cle: 'statut', label: 'Statut', valeur: (e) => e.statutLibelle },
  {
    cle: 'note',
    label: 'Note',
    classe: 'nombre',
    valeur: (e) => (e.note == null ? null : `${e.note}/100`),
    valeurTri: (e) => e.note,
  },
]
const triEntretiens = ref({ cle: 'date', sens: 1 })
const filtreEntretiens = reactive({ recherche: '', statut: '' })
const lignesEntretiens = computed(() =>
  props.entretiens.filter(
    (e) =>
      (!filtreEntretiens.statut || e.statut === filtreEntretiens.statut) &&
      contient(
        [e.id, e.candidat, ...e.interviewers.flatMap((i) => [i.detenteur, i.poste])].join(' '),
        filtreEntretiens.recherche,
      ),
  ),
)
</script>

<template>
  <div class="fiche fiche-offre">
    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Offre d'emploi</p>
        <h1>N° {{ o.id }} — {{ o.poste }}</h1>
        <p class="help">{{ o.departement }} · responsable : {{ o.responsable }}</p>
      </div>
      <div class="fiche-offre-actions">
        <span class="badge" :class="{ ok: ouverte }">{{ o.statusLibelle }}</span>
        <button
          type="button"
          :disabled="!ouverte"
          :title="
            ouverte
              ? undefined
              : `Offre ${o.statusLibelle.toLowerCase()} : plus d'entretien à préparer`
          "
          @click="preparer()"
        >
          Préparer un entretien
        </button>
      </div>
    </header>

    <div class="fiche-offre-haut">
      <section class="fiche-bloc">
        <h2>Informations</h2>
        <dl class="fiche-infos">
          <dt>Poste</dt>
          <dd>{{ o.poste }}</dd>
          <dt>Département</dt>
          <dd>{{ o.departement }}</dd>
          <dt>Responsable</dt>
          <dd>{{ o.responsable }}</dd>
          <dt>Date limite</dt>
          <dd>
            {{ o.delai ? dateLongue(o.delai) : 'Aucune' }}
            <span v-if="delai" class="badge petit" :class="delai.classe">{{ delai.texte }}</span>
          </dd>
          <dt>Demande d'origine</dt>
          <dd>
            <RouterLink :to="`/demandes/${o.idDemande}`">N° {{ o.idDemande }}</RouterLink>
            <span v-if="demande" class="help"> — {{ demande.motif }}</span>
          </dd>
        </dl>
      </section>

      <section class="fiche-bloc">
        <h2>Description</h2>
        <blockquote class="fiche-motif">{{ o.description }}</blockquote>
      </section>
    </div>

    <div class="board">
      <section class="colonne">
        <h2>
          Candidats <span class="count">{{ dossiers.length }}</span>
        </h2>
        <div class="filtres">
          <FiltrePuces
            v-model="filtreCandidats.puce"
            :elements="candidats"
            :code="(d) => (d.entretiens.length ? 'avec' : 'sans')"
            :options="PUCES_CANDIDATS"
            aria-label="Filtrer les candidats"
          />
          <BarreRecherche
            v-model="filtreCandidats.recherche"
            placeholder="Rechercher (nom, e-mail, pièce)…"
            aria-label="Rechercher dans les candidats"
          />
        </div>
        <Tableau
          v-model:tri="triCandidats"
          :lignes="lignesCandidats"
          :colonnes="COLONNES_CANDIDATS"
          :vide="dossiers.length ? 'Aucun candidat pour ce filtre' : 'Aucun dossier déposé'"
          :titre-ligne="(d) => [d.email, d.tel].filter(Boolean).join(' · ')"
        >
          <template #candidat="{ ligne }">
            {{ ligne.candidat }}
            <span class="cellule-detail">{{ ligne.employe ? 'Employé(e)' : 'Externe' }}</span>
          </template>
          <template #entretiens="{ ligne }">
            <RouterLink
              v-for="e in ligne.entretiens"
              :key="e.id"
              :to="lienEntretien(e)"
              class="cellule-entretien"
            >
              N° {{ e.id }}
              <span class="badge petit" :class="classeEntretien(e)">{{ e.statutLibelle }}</span>
            </RouterLink>
            <button
              type="button"
              class="bouton-petit"
              :disabled="!ouverte"
              :aria-label="`Préparer l'entretien de ${ligne.candidat}`"
              @click="preparer(ligne)"
            >
              Préparer
            </button>
          </template>
        </Tableau>
      </section>

      <section class="colonne">
        <h2>
          Entretiens <span class="count">{{ entretiens.length }}</span>
        </h2>
        <div class="filtres">
          <FiltrePuces
            v-model="filtreEntretiens.statut"
            :elements="entretiens"
            :code="(e) => e.statut"
            :libelle="(e) => e.statutLibelle"
            :ordre-alphabetique="false"
            aria-label="Filtrer les entretiens"
          />
          <BarreRecherche
            v-model="filtreEntretiens.recherche"
            placeholder="Rechercher (n°, candidat, interviewer)…"
            aria-label="Rechercher dans les entretiens"
          />
        </div>
        <Tableau
          v-model:tri="triEntretiens"
          :lignes="lignesEntretiens"
          :colonnes="COLONNES_ENTRETIENS"
          :vide="entretiens.length ? 'Aucun entretien pour ce filtre' : 'Aucun entretien préparé'"
          cliquable
          @ouvrir="(e) => router.push(lienEntretien(e))"
        >
          <template #id="{ ligne }">
            <RouterLink :to="lienEntretien(ligne)" @click.stop>{{ ligne.id }}</RouterLink>
          </template>
          <template #candidat="{ ligne }">
            {{ ligne.candidat }}
            <span class="cellule-detail">avec {{ interviewers(ligne) }}</span>
          </template>
          <template #statut="{ ligne }">
            <span class="badge petit" :class="classeEntretien(ligne)">{{
              ligne.statutLibelle
            }}</span>
          </template>
        </Tableau>
      </section>
    </div>

    <slot />
  </div>
</template>
