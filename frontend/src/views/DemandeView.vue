<script setup>
import { computed, reactive, ref, watch } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { departements, postes } from '../data/mock'

// demande(id, #idDepartement, #idStatus, date, limite, effectif)
// demandePoste(id, #idPoste, #idDemande) — cf. doc/conception/base/schema.md

// Statut attribué à la création (début du workflow Manager -> HR -> Direction -> Finance)
const ID_STATUS_INITIAL = 1

const aujourdhui = () => new Date().toISOString().slice(0, 10)
const vide = () => ({
  idDepartement: '',
  idPostes: [],
  effectif: 1,
  dateDemande: aujourdhui(),
  dateLimite: '',
})

const form = reactive(vide())
const erreur = ref('')
const message = ref('')

// Seuls les postes du département choisi sont proposés
const postesDuDepartement = computed(() =>
  postes.filter((p) => p.idDepartement === form.idDepartement),
)
watch(() => form.idDepartement, () => (form.idPostes = []))

function verifier() {
  if (form.dateLimite < form.dateDemande)
    return 'La date limite doit être postérieure ou égale à la date de demande.'
  if (!form.idPostes.length) return 'Sélectionnez au moins un poste.'
  return ''
}

function envoyer() {
  message.value = ''
  erreur.value = verifier()
  if (erreur.value) return

  // À remplacer par l'appel à l'API (idPostes -> lignes de demandePoste)
  console.log('Nouvelle demande', { ...form, idStatus: ID_STATUS_INITIAL })
  message.value = 'Demande enregistrée.'
  Object.assign(form, vide())
}
</script>

<template>
  <AppLayout title="Demande de personnel">
    <form class="form" @submit.prevent="envoyer">
      <label for="departement">Département</label>
      <select id="departement" v-model="form.idDepartement" required>
        <option value="" disabled>Choisir un département</option>
        <option v-for="d in departements" :key="d.id" :value="d.id">{{ d.label }}</option>
      </select>

      <fieldset>
        <legend>Postes concernés</legend>
        <p v-if="!form.idDepartement" class="help">Choisissez d'abord un département.</p>
        <label v-for="p in postesDuDepartement" :key="p.id" class="choice">
          <input v-model="form.idPostes" type="checkbox" :value="p.id" />
          {{ p.label }}
        </label>
      </fieldset>

      <label for="effectif">Effectif demandé</label>
      <input id="effectif" v-model.number="form.effectif" type="number" min="1" required />

      <label for="dateDemande">Date de demande</label>
      <input id="dateDemande" v-model="form.dateDemande" type="date" required />

      <label for="dateLimite">Date limite</label>
      <input id="dateLimite" v-model="form.dateLimite" type="date" :min="form.dateDemande" required />

      <p v-if="erreur" class="error">{{ erreur }}</p>
      <button type="submit">Envoyer la demande</button>
      <p v-if="message" class="success">{{ message }}</p>
    </form>
  </AppLayout>
</template>
