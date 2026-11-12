<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { droitEvaluer, heure } from '../../utils/entretien'
import { utilisateur } from '../../auth/session'
import { dateLongue } from '../../utils/dates'

// Fiche d'un entretien (api/entretiens.js, données statiques) : en-tête, chiffres, infos, offre,
// évaluations (une par interviewer) et, à droite, le bouton « Évaluer » puis les interviewers.
// Les classes .fiche-* viennent de la fiche demande (css/demande/FicheDemande.css).
const props = defineProps({ entretien: { type: Object, required: true } })
const e = computed(() => props.entretien)

const router = useRouter()

// Bouton « Évaluer » : grisé avec la raison si je ne suis pas interviewer ou si j'ai déjà évalué
const droit = computed(() => droitEvaluer(e.value, utilisateur.value))
const evaluer = () => router.push(`/entretiens/${e.value.id}/evaluation`)

const aEvalue = (i) => e.value.evaluations.some((v) => v.idPoste === i.idPoste)

// Lien vers la fiche de l'offre : grisé si la route n'existe pas ou m'est réservée (meta.acces du router)
const lienOffre = computed(() => `/offres/${e.value.idOffre}`)
const offreOuvrable = computed(() => {
  const r = router.resolve(lienOffre.value)
  return r.matched.length > 0 && (!r.meta.acces || r.meta.acces(utilisateur.value ?? {}))
})
</script>

<template>
  <div class="fiche">
    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Entretien de recrutement</p>
        <h1>N° {{ e.id }} — {{ e.candidat }}</h1>
        <p class="help">{{ e.poste }} · {{ e.departement }}</p>
      </div>
      <span class="badge" :class="e.statut === 'FINI' ? 'ok' : 'attente'">{{
        e.statutLibelle
      }}</span>
    </header>

    <!-- Chiffres clés -->
    <section class="fiche-chiffres">
      <div class="chiffre">
        <span class="chiffre-valeur">{{ dateLongue(e.date) }}</span>
        <span class="chiffre-label">à {{ heure(e.date) }}</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ e.evaluations.length }} / {{ e.interviewers.length }}</span>
        <span class="chiffre-label">évaluations rendues</span>
      </div>
      <div class="chiffre">
        <span class="chiffre-valeur">{{ e.note == null ? '—' : `${e.note}/100` }}</span>
        <span class="chiffre-label">{{
          e.notesCachees ? 'note moyenne : cachée jusqu’à votre évaluation' : 'note moyenne'
        }}</span>
      </div>
    </section>

    <div class="fiche-grille">
      <section class="fiche-bloc">
        <h2>Informations</h2>
        <dl class="fiche-infos">
          <dt>N° d'entretien</dt>
          <dd>{{ e.id }}</dd>
          <dt>Candidat</dt>
          <dd>{{ e.candidat }}</dd>
          <dt>Date</dt>
          <dd>{{ dateLongue(e.date) }}</dd>
          <dt>Heure</dt>
          <dd>{{ heure(e.date) }}</dd>
          <dt>Statut</dt>
          <dd>{{ e.statutLibelle }}</dd>
        </dl>
      </section>

      <section class="fiche-bloc">
        <h2>Offre</h2>
        <dl class="fiche-infos">
          <dt>Poste</dt>
          <dd>{{ e.poste }}</dd>
          <dt>Département</dt>
          <dd>{{ e.departement }}</dd>
          <dt>N° d'offre</dt>
          <dd>{{ e.idOffre }}</dd>
        </dl>
        <RouterLink v-if="offreOuvrable" :to="lienOffre" class="fiche-lien"
          >Voir l'offre →</RouterLink
        >
        <span v-else class="fiche-lien inactif" title="Fiche de l'offre non accessible">
          Voir l'offre → <span class="help">(non accessible)</span>
        </span>
      </section>

      <section class="fiche-bloc fiche-evaluations">
        <h2>
          Évaluations <span class="count">{{ e.evaluations.length }}</span>
          <span v-if="e.note != null" class="badge petit ok">Moyenne {{ e.note }}/100</span>
        </h2>
        <ul v-if="e.evaluations.length" class="evaluations">
          <li v-for="v in e.evaluations" :key="v.id">
            <div class="evaluation-tete">
              <span>
                <strong v-if="e.notesCachees">{{ v.detenteur }}</strong>
                <RouterLink v-else :to="`/evaluations/${v.id}`"
                  ><strong>{{ v.detenteur }}</strong></RouterLink
                >
                <span class="help"
                  >{{ v.poste }} · le {{ dateLongue(v.date) }} à {{ heure(v.date) }}</span
                >
              </span>
              <span v-if="e.notesCachees" class="badge petit attente">Note cachée</span>
              <span v-else class="evaluation-valeur">{{ v.note }}<small>/100</small></span>
            </div>
            <template v-if="!e.notesCachees">
              <span class="note-barre" aria-hidden="true"
                ><span :style="{ width: `${v.note}%` }"></span
              ></span>
              <blockquote class="fiche-motif">{{ v.remarques }}</blockquote>
            </template>
          </li>
        </ul>
        <p v-else class="vide">Aucune évaluation pour l'instant.</p>
        <p v-if="e.notesCachees && e.evaluations.length" class="help">
          Notes séparées : les notes et remarques des autres interviewers s'affichent quand vous
          avez rendu votre évaluation.
        </p>
      </section>

      <div class="fiche-cote">
        <section class="fiche-bloc decision">
          <h2>Mon évaluation</h2>
          <p class="help">
            {{
              droit.raison || 'Vous êtes interviewer de cet entretien : écrivez votre évaluation.'
            }}
          </p>
          <button type="button" :disabled="!!droit.raison" :title="droit.raison" @click="evaluer">
            Évaluer
          </button>
        </section>

        <section class="fiche-bloc fiche-interviewers">
          <h2>
            Interviewers <span class="count">{{ e.interviewers.length }}</span>
          </h2>
          <ul class="interviewers">
            <li v-for="i in e.interviewers" :key="i.idPoste" :class="{ moi: i === droit.moi }">
              <span>
                <strong>{{ i.detenteur }}</strong>
                <span class="help">{{ i.poste }}</span>
              </span>
              <span class="badge petit" :class="aEvalue(i) ? 'ok' : 'attente'">
                {{ aEvalue(i) ? 'A évalué' : 'Pas encore évalué' }}
              </span>
            </li>
          </ul>
        </section>
      </div>
    </div>

    <slot />
  </div>
</template>
