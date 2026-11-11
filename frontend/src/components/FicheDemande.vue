<script setup>
import { computed } from 'vue'
import Progression from './Progression.vue'
import {
  labelDepartement,
  lignesPostes,
  NATURES,
  totalPostes,
  joursRestants,
} from '../data/demandes'
import { ETAPES, codeEtape, labelEtape } from '../data/etapes'

// Fiche complète d'une demande (progression, en-tête, chiffres, infos, motif, postes, suivi).
// Utilisée par la page détail et par la validation.
// Slot « cote » : panneau affiché en haut de la colonne de droite, au-dessus du suivi
// (ex. les boutons Valider / Refuser).
const props = defineProps({ demande: { type: Object, required: true } })

const d = computed(() => props.demande)
const refusee = computed(() => (d.value?.etape === 'refusee' ? codeEtape(d.value.refuseePar) : ''))

// Statut affiché dans le badge de l'en-tête
const statut = computed(() => {
  if (!d.value) return null
  if (d.value.etape === 'approuvee') return { classe: 'ok', texte: 'Approuvée' }
  if (d.value.etape === 'refusee') return { classe: 'ko', texte: `Refusée par ${d.value.refuseePar}` }
  return { classe: 'attente', texte: `En attente ${labelEtape(d.value.etape)}` }
})


// Délai : texte + niveau d'urgence (moins de 2 semaines = proche)
const delai = computed(() => {
  const j = joursRestants(d.value)
  if (j === null) return { texte: 'Aucun délai fixé', classe: '' }
  if (j < 0) return { texte: `Dépassé de ${-j} jour(s)`, classe: 'ko' }
  return { texte: `${j} jour(s) restant(s)`, classe: j < 14 ? 'attente' : 'ok' }
})

// Suivi étape par étape, daté : création, puis date de décision de chaque étape (historique)
const suivi = computed(() => {
  const dem = d.value
  const i = dem.etape === 'approuvee' ? ETAPES.length : ETAPES.findIndex((e) => e.code === (refusee.value || dem.etape))
  const dateDe = (k) => (k === 0 ? dem.date : dem.historique?.[ETAPES[k].code])
  return ETAPES.map((e, k) => {
    const date = dateDe(k)
    if (k === 0) return { ...e, etat: 'faite', texte: 'Demande créée', date }
    if (k < i) return { ...e, etat: 'faite', texte: 'Validée', date }
    if (k === i && refusee.value) return { ...e, etat: 'refusee', texte: 'Refusée', date }
    if (k === i) return { ...e, etat: 'courante', texte: 'En attente depuis', date: dateDe(k - 1) }
    return { ...e, etat: 'a-venir', texte: refusee.value ? 'Non atteinte' : 'À venir', date: null }
  })
})

const dateLongue = (iso) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
</script>

<template>
  <div class="fiche">
    <Progression :etape="d.etape" :refusee="refusee" />

    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Demande de personnel</p>
        <h1>N° {{ d.id }} — {{ labelDepartement(d) }}</h1>
        <p class="help">Créée le {{ dateLongue(d.date) }}</p>
      </div>
      <span class="badge" :class="statut.classe">{{ statut.texte }}</span>
    </header>

    <!-- Chiffres clés -->
    <section class="fiche-chiffres">
      <div class="chiffre">
        <span class="chiffre-valeur">{{ totalPostes(d) }}</span>
        <span class="chiffre-label">postes demandés</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ joursRestants(d) === null ? '—' : Math.abs(joursRestants(d)) }}</span>
        <span class="chiffre-label">{{ joursRestants(d) < 0 ? 'jours de retard' : 'jours restants' }}</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ d.delai ? dateLongue(d.delai) : '—' }}</span>
        <span class="chiffre-label">date limite</span>
      </div>
    </section>

    <div class="fiche-grille">
      <section class="fiche-bloc">
        <h2>Informations</h2>
        <dl class="fiche-infos">
          <dt>N° de demande</dt><dd>{{ d.id }}</dd>
          <dt>Département</dt><dd>{{ labelDepartement(d) }}</dd>
          <dt>Date de création</dt><dd>{{ dateLongue(d.date) }}</dd>
          <dt>Délai</dt>
          <dd>
            {{ d.delai ? dateLongue(d.delai) : '—' }}
            <span v-if="delai.classe" class="badge petit" :class="delai.classe">{{ delai.texte }}</span>
            <span v-else class="help">{{ delai.texte }}</span>
          </dd>
          <dt>Statut</dt><dd>{{ statut.texte }}</dd>
        </dl>
      </section>

      <section class="fiche-bloc">
        <h2>Motif</h2>
        <blockquote class="fiche-motif">{{ d.motif }}</blockquote>
      </section>

      <section class="fiche-bloc fiche-postes">
        <h2>Postes demandés <span class="count">{{ totalPostes(d) }}</span></h2>
        <table class="table">
          <thead>
            <tr><th>Intitulé</th><th>Nature</th><th class="nombre">Effectif</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in lignesPostes(d)" :key="l.cle">
              <td>
                {{ l.label }}
                <span v-if="l.idPoste" class="help">(poste n° {{ l.idPoste }})</span>
              </td>
              <td><span class="badge petit nature" :class="l.nature">{{ NATURES[l.nature] }}</span></td>
              <td class="nombre">{{ l.effectif ?? '—' }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <div class="fiche-cote">
        <slot name="cote" />

        <section class="fiche-bloc fiche-suivi">
          <h2>Suivi</h2>
          <ol class="suivi">
            <li v-for="e in suivi" :key="e.code" :class="e.etat">
              <strong>{{ e.label }}</strong>
              <span>
                {{ e.texte }}<template v-if="e.date"> le <time :datetime="e.date">{{ dateLongue(e.date) }}</time></template>
              </span>
            </li>
          </ol>
        </section>
      </div>
    </div>

    <slot />
  </div>
</template>
