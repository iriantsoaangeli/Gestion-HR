<script setup>
import { computed } from 'vue'
import { heure } from '../../utils/entretien'
import { utilisateur } from '../../auth/session'
import { dateLongue } from '../../utils/dates'

// Fiche d'une évaluation (api/entretiens.js, données statiques) : la note et les remarques d'un
// interviewer, l'entretien évalué et, à droite, les autres évaluations du même entretien.
// Mise en page commune : .fiche-* (css/demande/FicheDemande.css), .note-barre et .interviewers
// (css/entretien/FicheEntretien.css).
const props = defineProps({ evaluation: { type: Object, required: true } })
const v = computed(() => props.evaluation)
const e = computed(() => props.evaluation.entretien)

// DONNÉES STATIQUES : mon évaluation = celle écrite par le poste de mon compte (retrouvé par le login)
const deMoi = computed(() => v.value.login === utilisateur.value?.login)

// Autres interviewers du même entretien : leur évaluation (lien) ou « pas encore évalué »
const autres = computed(() =>
  e.value.interviewers
    .filter((i) => i.idPoste !== v.value.idPoste)
    .map((i) => ({ ...i, evaluation: e.value.evaluations.find((x) => x.idPoste === i.idPoste) })),
)
</script>

<template>
  <div class="fiche">
    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Évaluation d'entretien</p>
        <h1>{{ e.candidat }} — {{ v.note }}/100</h1>
        <p class="help">
          par {{ v.detenteur }} ({{ v.poste }}) · entretien N° {{ e.id }} · {{ e.poste }}
        </p>
      </div>
      <span v-if="deMoi" class="badge ok">Mon évaluation</span>
    </header>

    <!-- Chiffres clés -->
    <section class="fiche-chiffres">
      <div class="chiffre">
        <span class="chiffre-valeur">{{ v.note }}/100</span>
        <span class="note-barre" aria-hidden="true"
          ><span :style="{ width: `${v.note}%` }"></span
        ></span>
        <span class="chiffre-label">note de {{ v.detenteur }}</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ e.note }}/100</span>
        <span class="chiffre-label">moyenne de l'entretien</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ e.evaluations.length }} / {{ e.interviewers.length }}</span>
        <span class="chiffre-label">évaluations rendues</span>
      </div>
    </section>

    <div class="fiche-grille">
      <section class="fiche-bloc">
        <h2>Évaluation</h2>
        <dl class="fiche-infos">
          <dt>Évaluateur</dt>
          <dd>{{ v.detenteur }}</dd>
          <dt>Poste</dt>
          <dd>{{ v.poste }}</dd>
          <dt>Date</dt>
          <dd>{{ dateLongue(v.date) }}</dd>
          <dt>Heure</dt>
          <dd>{{ heure(v.date) }}</dd>
          <dt>Note</dt>
          <dd>{{ v.note }}/100</dd>
        </dl>
      </section>

      <section class="fiche-bloc">
        <h2>Entretien</h2>
        <dl class="fiche-infos">
          <dt>Candidat</dt>
          <dd>{{ e.candidat }}</dd>
          <dt>Poste</dt>
          <dd>{{ e.poste }}</dd>
          <dt>Département</dt>
          <dd>{{ e.departement }}</dd>
          <dt>Date</dt>
          <dd>{{ dateLongue(e.date) }} à {{ heure(e.date) }}</dd>
          <dt>Statut</dt>
          <dd>
            <span class="badge petit" :class="e.statut === 'FINI' ? 'ok' : 'attente'">{{
              e.statutLibelle
            }}</span>
          </dd>
        </dl>
        <RouterLink :to="`/entretiens/${e.id}`" class="fiche-lien">Voir l'entretien →</RouterLink>
      </section>

      <section class="fiche-bloc fiche-remarques">
        <h2>Remarques</h2>
        <blockquote class="fiche-motif">{{ v.remarques }}</blockquote>
      </section>

      <div class="fiche-cote">
        <section class="fiche-bloc fiche-interviewers">
          <h2>
            Autres évaluations <span class="count">{{ autres.length }}</span>
          </h2>
          <ul v-if="autres.length" class="interviewers">
            <li v-for="i in autres" :key="i.idPoste">
              <span>
                <strong>{{ i.detenteur }}</strong>
                <span class="help">{{ i.poste }}</span>
              </span>
              <RouterLink
                v-if="i.evaluation"
                :to="`/evaluations/${i.evaluation.id}`"
                class="badge petit ok"
                >{{ i.evaluation.note }}/100</RouterLink
              >
              <span v-else class="badge petit attente">Pas encore évalué</span>
            </li>
          </ul>
          <p v-else class="vide">Seul interviewer de cet entretien.</p>
        </section>
      </div>
    </div>

    <slot />
  </div>
</template>
