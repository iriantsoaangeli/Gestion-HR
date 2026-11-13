<script setup>
import { computed, reactive, ref } from 'vue'
import ChampNombre from '../ui/ChampNombre.vue'
import Erreur from '../ui/Erreur.vue'
import * as api from '../../api/entretiens'
import { dateLongue } from '../../utils/dates'
import { droitEvaluer, heure } from '../../utils/entretien'

// Évaluation d'un entretien par l'interviewer connecté : rappel de l'entretien, puis note /100 et
// remarques. Chaque interviewer écrit sa propre évaluation (doc/conception/regle-metier/bloc2.md).
// Formulaire grisé avec la raison si je ne suis pas interviewer ou si j'ai déjà évalué.
// Émet « evaluee » avec l'entretien mis à jour.
const props = defineProps({ entretien: { type: Object, required: true } })
const emit = defineEmits(['evaluee'])

const e = computed(() => props.entretien)
const droit = computed(() => droitEvaluer(e.value))
const aEvalue = (i) => e.value.evaluations.some((v) => v.idPoste === i.idPoste)

const form = reactive({ note: null, remarques: '' })
const erreur = ref('')
const envoi = ref(false)

// Note entière de 0 à 100, remarques obligatoires
const noteValide = computed(() => Number.isInteger(form.note) && form.note >= 0 && form.note <= 100)
const valide = computed(() => noteValide.value && form.remarques.trim() !== '')

async function envoyer() {
  if (!valide.value || droit.value.raison) return
  envoi.value = true
  erreur.value = ''
  try {
    const maj = await api.evaluer(e.value.id, {
      note: form.note,
      remarques: form.remarques.trim(),
    })
    emit('evaluee', maj)
  } catch (x) {
    erreur.value = x.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="evaluation">
    <header class="fiche-entete">
      <div>
        <p class="fiche-sur-titre">Évaluation d'entretien</p>
        <h1>N° {{ e.id }} — {{ e.candidat }}</h1>
        <p class="help">{{ e.poste }} · {{ e.departement }}</p>
      </div>
      <span class="badge" :class="e.fini ? 'ok' : 'attente'">{{ e.statutLibelle }}</span>
    </header>

    <div class="evaluation-grille">
      <section class="fiche-bloc">
        <h2>Rappel de l'entretien</h2>
        <dl class="fiche-infos">
          <dt>Candidat</dt>
          <dd>{{ e.candidat }}</dd>
          <dt>Poste</dt>
          <dd>{{ e.poste }}</dd>
          <dt>Département</dt>
          <dd>{{ e.departement }}</dd>
          <dt>Date</dt>
          <dd>{{ dateLongue(e.date) }} à {{ heure(e.date) }}</dd>
        </dl>

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

      <form class="fiche-bloc evaluation-form" @submit.prevent="envoyer">
        <h2>Mon évaluation</h2>
        <p v-if="droit.raison" class="evaluation-raison">{{ droit.raison }}</p>
        <p v-else class="help">
          En tant que {{ droit.moi.poste }} ({{ droit.moi.detenteur }}). Les autres interviewers
          écrivent chacun la leur.
        </p>

        <fieldset :disabled="!!droit.raison || envoi">
          <label for="note">Note sur 100</label>
          <div class="evaluation-note">
            <ChampNombre
              id="note"
              v-model="form.note"
              :min="0"
              :max="100"
              :pas="5"
              libelle="la note"
              step="1"
              required
              placeholder="0-100"
            />
            <span class="note-barre" aria-hidden="true">
              <span :style="{ width: `${noteValide ? form.note : 0}%` }"></span>
            </span>
          </div>
          <p v-if="form.note !== null && form.note !== '' && !noteValide" class="error">
            La note est un nombre entier de 0 à 100.
          </p>

          <label for="remarques">Remarques</label>
          <textarea
            id="remarques"
            v-model="form.remarques"
            rows="8"
            required
            placeholder="Points forts, points faibles, avis sur le candidat…"
          ></textarea>
          <p v-if="noteValide && !form.remarques.trim()" class="help">
            Les remarques sont obligatoires pour enregistrer l'évaluation.
          </p>
        </fieldset>

        <Erreur :message="erreur" />

        <div class="actions">
          <button type="submit" :disabled="!!droit.raison || !valide || envoi">
            {{ envoi ? 'Enregistrement…' : "Enregistrer l'évaluation" }}
          </button>
          <RouterLink :to="`/entretiens/${e.id}`" class="evaluation-annuler">{{
            droit.raison ? "Retour à l'entretien" : 'Annuler'
          }}</RouterLink>
        </div>
      </form>
    </div>

    <slot />
  </div>
</template>
