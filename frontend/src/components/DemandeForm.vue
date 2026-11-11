<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { departements, postes } from '../data/mock'
import { ajouterDemande } from '../data/demandes'

// demande(id, #idDepartement, #idStatus, date, effectif, motif)
// demandePoste(id, #idPoste, #idDemande) — cf. doc/conception/base/schema.md

// Statut attribué à la création (début du workflow Manager -> HR -> Direction -> Finance)
const ID_STATUS_INITIAL = 1

// Date du jour au format AAAA-MM-JJ
const aujourdhui = () => new Date().toISOString().slice(0, 10)

const router = useRouter()
const form = reactive({ idDepartement: '', idPostes: [], effectif: 1, motif: '' })
const erreur = ref('')

// Seuls les postes du département choisi sont proposés
const postesDuDepartement = computed(() =>
  postes.filter((p) => p.idDepartement === form.idDepartement),
)
watch(() => form.idDepartement, () => (form.idPostes = []))

function envoyer() {
  erreur.value = form.idPostes.length ? '' : 'Sélectionnez au moins un poste.'
  if (erreur.value) return

  // À remplacer par l'appel à l'API (idPostes -> lignes de demandePoste, date = NOW() en base)
  const demande = { ...form, idStatus: ID_STATUS_INITIAL, date: aujourdhui() }
  console.log('Nouvelle demande', demande)
  router.push(`/demande/hr?id=${ajouterDemande(demande)}`)
}
</script>

<template>
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

    <label for="motif">Motif</label>
    <textarea id="motif" v-model.trim="form.motif" rows="4" maxlength="200" required></textarea>

    <p v-if="erreur" class="error">{{ erreur }}</p>
    <button type="submit">Envoyer la demande</button>
  </form>
</template>
