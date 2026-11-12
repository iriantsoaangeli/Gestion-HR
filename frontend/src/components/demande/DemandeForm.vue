<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import RechercheSelect from '../ui/RechercheSelect.vue'
import Erreur from '../ui/Erreur.vue'
import * as referentiel from '../../api/referentiel'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'
import { dansLe } from '../../utils/dates'

// Formulaire de création (POST /api/demandes). Département, fonctions et postes viennent du backend.
// Département : celui du manager connecté (son poste), non modifiable.
// creations : postes à créer = une fonction + un effectif (le poste n'existe pas encore)
// remplacements : postes existants du département à remplir (1 poste = 1 siège, pas d'effectif) ;
//   le backend ne propose pas les postes des autres comptes (managers, supérieurs), mais bien le mien
// Le backend revérifie tout (règles dans DemandeService) : ses erreurs s'affichent dans <Erreur>.

// Délai : 3 mois par défaut, au moins 1 semaine
const DELAI_MIN = dansLe({ jours: 7 })
const DELAI_DEFAUT = dansLe({ mois: 3 })

const router = useRouter()
const form = reactive({
  idDepartement: null,
  creations: [], // [{ idFonction, label, effectif }]
  remplacements: [], // [{ id, label }]
  motif: '',
  avecDelai: false,
  delai: DELAI_DEFAUT,
})
const erreur = ref('')
const envoi = ref(false)

const postesDuDepartement = ref([])
// Mon département, puis ses postes remplaçables
const departement = useChargement(async () => {
  const d = await referentiel.monDepartement()
  form.idDepartement = d.id
  postesDuDepartement.value = await referentiel.postes(d.id)
  return d
}, null)
const { donnees: fonctions, erreur: erreurFonctions } = useChargement(referentiel.fonctions, [])

// Fonctions pas encore ajoutées (l'effectif sert à en demander plusieurs)
const fonctionsDisponibles = computed(() =>
  fonctions.value.filter((f) => !form.creations.some((c) => c.idFonction === f.id)),
)

function ajouterCreation(fonction) {
  form.creations.push({ idFonction: fonction.id, label: fonction.label, effectif: 1 })
}

// Postes existants pas encore choisis
const postesRemplacables = computed(() =>
  postesDuDepartement.value.filter((p) => !form.remplacements.some((r) => r.id === p.id)),
)

async function envoyer() {
  const vide = !form.creations.length && !form.remplacements.length
  const delaiTropCourt = form.avecDelai && form.delai < DELAI_MIN
  erreur.value = vide
    ? 'Ajoutez ou remplacez au moins un poste.'
    : delaiTropCourt
      ? "Le délai doit être d'au moins une semaine."
      : ''
  if (erreur.value) return

  envoi.value = true
  try {
    const creee = await api.creer({
      idDepartement: form.idDepartement,
      motif: form.motif,
      delai: form.avecDelai ? form.delai : null,
      creations: form.creations.map(({ idFonction, effectif }) => ({ idFonction, effectif })),
      remplacements: form.remplacements.map((r) => r.id),
    })
    // Le manager ne valide pas : il arrive sur la fiche de sa demande, qui part chez HR
    router.push(`/demandes/${creee.id}`)
  } catch (e) {
    erreur.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <form class="form demande-form" @submit.prevent="envoyer">
    <div class="form-col">
      <label for="departement">Département</label>
      <!-- Pas de choix : un manager ne fait de demande que pour son département -->
      <input
        id="departement"
        :value="departement.donnees.value?.label ?? (departement.chargement.value ? 'Chargement…' : '')"
        readonly
        aria-describedby="departement-aide"
      />
      <p id="departement-aide" class="help">Votre département : vos demandes ne concernent que lui.</p>

      <Erreur :message="departement.erreur.value || erreurFonctions" />

      <fieldset :disabled="!form.idDepartement">
        <legend><label for="ajout-poste">Ajouter un poste</label></legend>
        <p class="help">Nouveau poste : choisissez sa fonction et combien de postes créer.</p>
        <RechercheSelect
          id="ajout-poste"
          :options="fonctionsDisponibles"
          placeholder="Rechercher une fonction…"
          vide="Aucune autre fonction."
          @choisir="ajouterCreation"
        />
        <div v-for="(a, i) in form.creations" :key="a.idFonction" class="poste-ligne">
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
          <button type="button" class="btn-danger" :aria-label="`Retirer ${a.label}`" @click="form.creations.splice(i, 1)">
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
          @choisir="(p) => form.remplacements.push(p)"
        />
        <ul v-if="form.remplacements.length" class="selection">
          <li v-for="(p, i) in form.remplacements" :key="p.id">
            {{ p.label }}
            <button type="button" :aria-label="`Retirer ${p.label}`" @click="form.remplacements.splice(i, 1)">
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
      <button type="submit" :disabled="envoi">{{ envoi ? 'Envoi…' : 'Envoyer la demande' }}</button>
    </div>
  </form>
</template>
