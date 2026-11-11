<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { departements, postes } from '../data/mock'
import { ajouterDemande, labelPoste } from '../data/demandes'

// demande(id, #idDepartement, #idStatus, date, motif)
// demandePoste(id, #idPoste, #idDemande, effectif) — cf. doc/conception/base/schema.md

// Statut attribué à la création (début du workflow Manager -> HR -> Direction -> Finance)
const ID_STATUS_INITIAL = 1

// Date du jour au format AAAA-MM-JJ
const aujourdhui = () => new Date().toISOString().slice(0, 10)

const router = useRouter()
// postes : [{ idPoste, effectif }] — un effectif par poste sélectionné
const form = reactive({ idDepartement: '', postes: [], motif: '' })
const recherche = ref('')
const erreur = ref('')

// Postes du département, pas encore sélectionnés, dont le libellé contient la recherche
const suggestions = computed(() => {
  const texte = recherche.value.trim().toLowerCase()
  return postes.filter(
    (p) =>
      p.idDepartement === form.idDepartement &&
      !form.postes.some((s) => s.idPoste === p.id) &&
      p.label.toLowerCase().includes(texte),
  )
})

// Changer de département vide la sélection
watch(
  () => form.idDepartement,
  () => {
    form.postes = []
    recherche.value = ''
  },
)

function ajouterPoste(poste) {
  form.postes.push({ idPoste: poste.id, effectif: 1 })
  recherche.value = ''
}

// Entrée dans la recherche : ajoute la première suggestion au lieu d'envoyer le formulaire
function ajouterPremier() {
  if (suggestions.value.length) ajouterPoste(suggestions.value[0])
}

function retirerPoste(index) {
  form.postes.splice(index, 1)
}

function envoyer() {
  erreur.value = form.postes.length ? '' : 'Sélectionnez au moins un poste.'
  if (erreur.value) return

  // À remplacer par l'appel à l'API (postes -> lignes de demandePoste, date = NOW() en base)
  const demande = { ...form, postes: form.postes.map((p) => ({ ...p })), idStatus: ID_STATUS_INITIAL, date: aujourdhui() }
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

      <template v-else>
        <input
          v-model="recherche"
          type="search"
          placeholder="Rechercher un poste…"
          aria-label="Rechercher un poste"
          @keydown.enter.prevent="ajouterPremier"
        />
        <ul v-if="suggestions.length" class="suggestions">
          <li v-for="p in suggestions" :key="p.id">
            <button type="button" @click="ajouterPoste(p)">+ {{ p.label }}</button>
          </li>
        </ul>
        <p v-else class="help">Aucun autre poste à ajouter.</p>

        <div v-for="(p, i) in form.postes" :key="p.idPoste" class="poste-ligne">
          <label :for="`effectif-${p.idPoste}`">{{ labelPoste(p.idPoste) }}</label>
          <input
            :id="`effectif-${p.idPoste}`"
            v-model.number="p.effectif"
            type="number"
            min="1"
            required
            aria-label="Effectif"
          />
          <button type="button" class="btn-danger" :aria-label="`Retirer ${labelPoste(p.idPoste)}`" @click="retirerPoste(i)">
            ✕
          </button>
        </div>
      </template>
    </fieldset>

    <label for="motif">Motif</label>
    <textarea id="motif" v-model.trim="form.motif" rows="4" maxlength="200" required></textarea>

    <p v-if="erreur" class="error">{{ erreur }}</p>
    <button type="submit">Envoyer la demande</button>
  </form>
</template>
