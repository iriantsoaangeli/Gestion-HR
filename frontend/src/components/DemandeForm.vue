<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import RechercheSelect from './RechercheSelect.vue'
import Erreur from './Erreur.vue'
import { departements, postes } from '../data/mock'
import { ajouterDemande } from '../data/demandes'

// demande(id, #idDepartement, #idStatus, date, motif, delai) — delai : date limite, NULL si aucun
// demandePoste(id, #idPoste, #idDemande) — 1 poste = 1 siège, donc pas d'effectif
// cf. doc/conception/base/schema.md

// Statut attribué à la création (début du workflow Manager -> HR -> Direction -> Finance)
const ID_STATUS_INITIAL = 1

// Date au format AAAA-MM-JJ (heure locale, pas UTC)
const iso = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// Aujourd'hui décalé de quelques jours / mois
function dansLe({ jours = 0, mois = 0 } = {}) {
  const d = new Date()
  d.setMonth(d.getMonth() + mois)
  d.setDate(d.getDate() + jours)
  return iso(d)
}

const aujourdhui = () => dansLe()
// Délai : 3 mois par défaut, au moins 1 semaine
const DELAI_MIN = dansLe({ jours: 7 })
const DELAI_DEFAUT = dansLe({ mois: 3 })

const router = useRouter()
// ajouts : postes à créer [{ label, effectif }] — l'effectif ne concerne QUE la création
//   (effectif = nombre de sièges à créer pour cet intitulé)
// remplacements : id des postes existants à remplir ou remplacer (1 poste = 1 siège, pas d'effectif)
const form = reactive({
  idDepartement: '',
  ajouts: [],
  remplacements: [],
  motif: '',
  avecDelai: false,
  delai: DELAI_DEFAUT,
})
const erreur = ref('')

const postesDuDepartement = computed(() =>
  postes.filter((p) => p.idDepartement === form.idDepartement),
)

// Suggestions pour un nouveau poste : libellés déjà utilisés dans le département, sans doublon
const libellesDuDepartement = computed(() =>
  [...new Set(postesDuDepartement.value.map((p) => p.label))].map((label) => ({ id: label, label })),
)

// Intitulés pas encore ajoutés (l'effectif sert à en demander plusieurs)
const libellesDisponibles = computed(() =>
  libellesDuDepartement.value.filter((o) => !form.ajouts.some((a) => a.label === o.label)),
)

// Un intitulé déjà ajouté (même saisi à la main) augmente l'effectif au lieu de dupliquer la ligne
function ajouterPoste(label) {
  const existant = form.ajouts.find((a) => a.label.toLowerCase() === label.toLowerCase())
  if (existant) existant.effectif++
  else form.ajouts.push({ label, effectif: 1 })
}

// Postes existants pas encore choisis
const postesRemplacables = computed(() =>
  postesDuDepartement.value.filter((p) => !form.remplacements.includes(p.id)),
)

const labelPoste = (id) => postes.find((p) => p.id === id)?.label

// Changer de département vide la sélection
watch(
  () => form.idDepartement,
  () => {
    form.ajouts = []
    form.remplacements = []
  },
)

function envoyer() {
  const vide = !form.ajouts.length && !form.remplacements.length
  const delaiTropCourt = form.avecDelai && form.delai < DELAI_MIN
  erreur.value = vide
    ? 'Ajoutez ou remplacez au moins un poste.'
    : delaiTropCourt
      ? 'Le délai doit être d\'au moins une semaine.'
      : ''
  if (erreur.value) return

  // À remplacer par l'appel à l'API : chaque ajout crée « effectif » postes (poste, affectation,
  // creationPoste), puis chaque poste donne une ligne de demandePoste. date = NOW() en base.
  const demande = {
    idDepartement: form.idDepartement,
    motif: form.motif,
    delai: form.avecDelai ? form.delai : null,
    ajouts: form.ajouts.map((a) => ({ ...a })),
    remplacements: [...form.remplacements],
    idStatus: ID_STATUS_INITIAL,
    date: aujourdhui(),
  }
  console.log('Nouvelle demande', demande)
  router.push(`/demande/hr?id=${ajouterDemande(demande)}`)
}
</script>

<template>
  <form class="form demande-form" @submit.prevent="envoyer">
    <div class="form-col">
      <label for="departement">Département</label>
      <select
        id="departement"
        v-model="form.idDepartement"
        :class="{ 'a-remplir': !form.idDepartement }"
        aria-describedby="departement-aide"
        required
      >
        <option value="" disabled>Choisir un département</option>
        <option v-for="d in departements" :key="d.id" :value="d.id">{{ d.label }}</option>
      </select>

      <!-- Les postes restent visibles mais grisés et inutilisables tant qu'aucun département n'est choisi -->
      <p v-if="!form.idDepartement" id="departement-aide" class="a-remplir-aide">
        Choisissez d'abord un département pour ajouter ou remplacer des postes.
      </p>

      <fieldset :disabled="!form.idDepartement">
        <legend><label for="ajout-poste">Ajouter un poste</label></legend>
        <p class="help">Nouveau poste : indiquez combien de sièges créer.</p>
        <RechercheSelect
          id="ajout-poste"
          :options="libellesDisponibles"
          creation
          placeholder="Rechercher ou saisir un intitulé…"
          vide="Saisissez un intitulé de poste."
          @choisir="(o) => ajouterPoste(o.label)"
        />
        <div v-for="(a, i) in form.ajouts" :key="a.label" class="poste-ligne">
          <span>{{ a.label }}</span>
          <label :for="`effectif-${i}`">Effectif</label>
          <div class="stepper">
            <button
              type="button"
              :disabled="a.effectif <= 1"
              :aria-label="`Diminuer l'effectif de ${a.label}`"
              @click="a.effectif--"
            >
              <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10" /></svg>
            </button>
            <input :id="`effectif-${i}`" v-model.number="a.effectif" type="number" min="1" required />
            <button type="button" :aria-label="`Augmenter l'effectif de ${a.label}`" @click="a.effectif++">
              <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h10M8 3v10" /></svg>
            </button>
          </div>
          <button type="button" class="btn-danger" :aria-label="`Retirer ${a.label}`" @click="form.ajouts.splice(i, 1)">
            ✕
          </button>
        </div>
      </fieldset>

      <fieldset :disabled="!form.idDepartement">
        <legend><label for="remplacement-poste">Remplir / remplacer un poste</label></legend>
        <p class="help">Un poste existant, vacant ou dont l'occupant part.</p>
        <RechercheSelect
          id="remplacement-poste"
          :options="postesRemplacables"
          placeholder="Rechercher un poste existant…"
          vide="Aucun autre poste dans ce département."
          @choisir="(o) => form.remplacements.push(o.id)"
        />
        <ul v-if="form.remplacements.length" class="selection">
          <li v-for="(id, i) in form.remplacements" :key="id">
            {{ labelPoste(id) }}
            <button type="button" :aria-label="`Retirer ${labelPoste(id)}`" @click="form.remplacements.splice(i, 1)">
              ✕
            </button>
          </li>
        </ul>
      </fieldset>
    </div>

    <div class="form-col">
      <label for="motif">Motif</label>
      <textarea id="motif" v-model.trim="form.motif" rows="8" maxlength="200" required></textarea>

      <label class="choice choice-grande">
        <input v-model="form.avecDelai" type="checkbox" />
        Ajouter un délai
      </label>
      <template v-if="form.avecDelai">
        <label for="delai">Date limite</label>
        <input id="delai" v-model="form.delai" type="date" :min="DELAI_MIN" required />
        <p class="help">3 mois par défaut, au moins une semaine.</p>
      </template>

      <Erreur :message="erreur" fermable @fermer="erreur = ''" />
      <button type="submit">Envoyer la demande</button>
    </div>
  </form>
</template>
