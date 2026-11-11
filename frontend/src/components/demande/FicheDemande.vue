<script setup>
import { computed } from 'vue'
import Progression from '../ui/Progression.vue'
import { lignesPostes, NATURES, totalPostes, texteStatut, classeStatut } from '../../utils/demande'
import { dateLongue, joursAvant } from '../../utils/dates'

// Fiche complète d'une demande (DemandeResponse de l'API) : progression, en-tête, chiffres, infos,
// motif, postes, suivi. Le suivi et la progression sont calculés par le backend (demande.suivi).
// Slot « cote » : panneau en haut de la colonne de droite, au-dessus du suivi (ex. la décision).
const props = defineProps({ demande: { type: Object, required: true } })
const d = computed(() => props.demande)

const jours = computed(() => joursAvant(d.value.delai))

// Délai : texte + niveau d'urgence (moins de 2 semaines = proche)
const delai = computed(() => {
  const j = jours.value
  if (j === null) return { texte: 'Aucun délai fixé', classe: '' }
  if (j < 0) return { texte: `Dépassé de ${-j} jour(s)`, classe: 'ko' }
  return { texte: `${j} jour(s) restant(s)`, classe: j < 14 ? 'attente' : 'ok' }
})
</script>

<template>
  <div class="fiche">
    <Progression :etapes="d.suivi" />

    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Demande de personnel</p>
        <h1>N° {{ d.id }} — {{ d.departement }}</h1>
        <p class="help">Créée le {{ dateLongue(d.date) }} par {{ d.demandeur }}</p>
      </div>
      <span class="badge" :class="classeStatut(d)">{{ texteStatut(d) }}</span>
    </header>

    <!-- Chiffres clés -->
    <section class="fiche-chiffres">
      <div class="chiffre">
        <span class="chiffre-valeur">{{ totalPostes(d) }}</span>
        <span class="chiffre-label">postes demandés</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ jours === null ? '—' : Math.abs(jours) }}</span>
        <span class="chiffre-label">{{ jours < 0 ? 'jours de retard' : 'jours restants' }}</span>
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
          <dt>Département</dt><dd>{{ d.departement }}</dd>
          <dt>Demandeur</dt><dd>{{ d.demandeur }}</dd>
          <dt>Date de création</dt><dd>{{ dateLongue(d.date) }}</dd>
          <dt>Délai</dt>
          <dd>
            {{ d.delai ? dateLongue(d.delai) : '—' }}
            <span v-if="delai.classe" class="badge petit" :class="delai.classe">{{ delai.texte }}</span>
            <span v-else class="help">{{ delai.texte }}</span>
          </dd>
          <dt>Statut</dt><dd>{{ texteStatut(d) }}</dd>
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
            <li v-for="e in d.suivi" :key="e.libelle" :class="e.etat">
              <strong>{{ e.libelle }}</strong>
              <span>
                {{ e.texte }}<template v-if="e.date"> le <time :datetime="e.date">{{ dateLongue(e.date) }}</time></template>
                <template v-if="e.utilisateur"> · {{ e.utilisateur }}</template>
              </span>
            </li>
          </ol>
        </section>
      </div>
    </div>

    <slot />
  </div>
</template>
