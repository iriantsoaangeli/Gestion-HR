<script setup>
import { computed, reactive, ref } from 'vue'
import Erreur from '../ui/Erreur.vue'
import RechercheSelect from '../ui/RechercheSelect.vue'
import * as api from '../../api/dossiers'
import { useChargement } from '../../utils/chargement'
import { dateLongue, iso } from '../../utils/dates'
import { normaliser } from '../../utils/texte'

// Dépôt d'un dossier de candidature (HR) : une offre ouverte, un candidat, des pièces jointes.
// À gauche : l'offre, puis les fichiers (30 Mo au total). À droite : le candidat,
// choisi dans la base (employé ou externe) ou ajouté avec le formulaire « Nouvelle personne ».
// Données statiques (api/dossiers.js) : rien n'est encore envoyé au backend.

const TAILLE_MAX = 30 * 1024 * 1024 // 30 Mo pour tout le dossier
const AUJOURDHUI = iso(new Date())

const { donnees: personnes, erreur: erreurPersonnes } = useChargement(api.personnes, [])
const { donnees: offres, erreur: erreurOffres } = useChargement(api.offresOuvertes, [])

// ---------- Offre ----------

const idOffre = ref('')
const offre = computed(() => offres.value.find((o) => o.id === idOffre.value) ?? null)

// ---------- Candidat ----------

const mode = ref('existante') // 'existante' : choisi dans la base ; 'nouvelle' : ajouté ici
const idPersonne = ref(null)
const personne = computed(() => personnes.value.find((p) => p.id === idPersonne.value) ?? null)

const nomComplet = (p) => `${p.prenom} ${p.nom}`
// Suggestions de la recherche : on précise tout de suite si la personne est employée
const options = computed(() =>
  personnes.value.map((p) => ({
    id: p.id,
    label: `${nomComplet(p)} — ${p.poste ? `employé(e) : ${p.poste}` : 'externe'}`,
  })),
)

const vide = () => ({ nom: '', prenom: '', naissance: '', adresse: '', tel: '', email: '' })
const nouvelle = reactive(vide())

// Évite les doublons : même e-mail, ou même nom + prénom + date de naissance qu'une personne de la base
const doublon = computed(() => {
  if (mode.value !== 'nouvelle') return null
  const email = nouvelle.email.trim().toLowerCase()
  return (
    personnes.value.find(
      (p) =>
        (email && p.email.toLowerCase() === email) ||
        (nouvelle.naissance &&
          p.naissance === nouvelle.naissance &&
          normaliser(p.nom) === normaliser(nouvelle.nom.trim()) &&
          normaliser(p.prenom) === normaliser(nouvelle.prenom.trim())),
    ) ?? null
  )
})

function choisirDoublon() {
  idPersonne.value = doublon.value.id
  mode.value = 'existante'
  Object.assign(nouvelle, vide())
}

// ---------- Pièces jointes ----------

const fichiers = ref([])
const refuses = ref([]) // messages des fichiers écartés au dernier ajout
const survol = ref(false) // un fichier est glissé au-dessus de la zone
const champFichiers = ref(null)

const total = computed(() => fichiers.value.reduce((s, f) => s + f.size, 0))
const pourcentage = computed(() => Math.min(100, Math.round((total.value / TAILLE_MAX) * 100)))

const taille = (octets) =>
  octets < 1024 * 1024
    ? `${Math.max(1, Math.round(octets / 1024))} Ko`
    : `${(octets / 1024 / 1024).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Mo`

function ajouter(liste) {
  refuses.value = []
  let somme = total.value
  for (const f of liste) {
    if (fichiers.value.some((x) => x.name === f.name && x.size === f.size)) {
      refuses.value.push(`« ${f.name} » est déjà joint.`)
    } else if (somme + f.size > TAILLE_MAX) {
      refuses.value.push(`« ${f.name} » (${taille(f.size)}) dépasse la limite de 30 Mo du dossier.`)
    } else {
      fichiers.value.push(f)
      somme += f.size
    }
  }
}

function choisirFichiers(e) {
  ajouter([...e.target.files])
  e.target.value = '' // permet de reprendre le même fichier après l'avoir retiré
}

function deposerFichiers(e) {
  survol.value = false
  ajouter([...e.dataTransfer.files])
}

const retirer = (i) => fichiers.value.splice(i, 1)

// ---------- Envoi ----------

const erreur = ref('')
const envoye = ref(null)

function deposer() {
  erreur.value = !offre.value
    ? 'Choisissez une offre.'
    : mode.value === 'existante' && !personne.value
      ? 'Choisissez un candidat dans la base, ou ajoutez une nouvelle personne.'
      : doublon.value
        ? `Cette personne existe déjà (${nomComplet(doublon.value)}) : sélectionnez-la au lieu de la recréer.`
        : !fichiers.value.length
          ? 'Joignez au moins un fichier (CV, lettre de motivation…).'
          : ''
  if (erreur.value) return

  // Pas encore de backend : on affiche seulement le récapitulatif
  const candidat = mode.value === 'existante' ? personne.value : { ...nouvelle, poste: null }
  envoye.value = {
    offre: offre.value.intitule,
    candidat: nomComplet(candidat),
    employe: Boolean(candidat.poste),
    nouvelle: mode.value === 'nouvelle',
    fichiers: fichiers.value.length,
  }
  idPersonne.value = null
  Object.assign(nouvelle, vide())
  fichiers.value = []
  refuses.value = []
}
</script>

<template>
  <form class="dossier" @submit.prevent="deposer">
    <!-- À gauche : l'offre et les pièces jointes -->
    <div class="dossier-colonne">
      <fieldset class="dossier-bloc">
        <legend>Offre</legend>
        <label for="dossier-offre">Offre ouverte</label>
        <select id="dossier-offre" v-model="idOffre" required :class="{ 'a-choisir': !idOffre }">
          <option value="" disabled>Choisir une offre…</option>
          <option v-for="o in offres" :key="o.id" :value="o.id">
            {{ o.intitule }} — {{ o.departement }}
          </option>
        </select>
        <Erreur :message="erreurOffres" />

        <dl v-if="offre" class="dossier-rappel">
          <dt>Poste</dt>
          <dd>{{ offre.poste }}</dd>
          <dt>Département</dt>
          <dd>{{ offre.departement }}</dd>
          <dt>Contrat</dt>
          <dd>{{ offre.contrat }}</dd>
          <dt>Date limite</dt>
          <dd>{{ offre.delai ? dateLongue(offre.delai) : 'Aucune' }}</dd>
          <dt>Demande</dt>
          <dd>N° {{ offre.idDemande }}</dd>
        </dl>
      </fieldset>

      <fieldset class="dossier-bloc">
        <legend>Pièces jointes</legend>

        <!-- Zone de dépôt : glisser des fichiers, ou cliquer pour les choisir -->
        <div
          class="dossier-depot"
          :class="{ survol }"
          role="button"
          tabindex="0"
          @click="champFichiers.click()"
          @keydown.enter.prevent="champFichiers.click()"
          @keydown.space.prevent="champFichiers.click()"
          @dragover.prevent="survol = true"
          @dragleave="survol = false"
          @drop.prevent="deposerFichiers"
        >
          <strong>Glissez les fichiers ici</strong>
          <span class="help">ou cliquez pour les choisir — 30 Mo au total</span>
        </div>
        <input
          ref="champFichiers"
          class="dossier-champ"
          type="file"
          multiple
          @change="choisirFichiers"
        />

        <div class="dossier-jauge" :class="{ pleine: pourcentage >= 90 }">
          <div class="dossier-jauge-barre" :style="{ width: `${pourcentage}%` }"></div>
        </div>
        <p class="help">{{ taille(total) }} sur 30 Mo · {{ fichiers.length }} fichier(s)</p>

        <ul v-if="fichiers.length" class="dossier-fichiers">
          <li v-for="(f, i) in fichiers" :key="`${f.name}-${f.size}`">
            <span class="dossier-fichier-nom" :title="f.name">{{ f.name }}</span>
            <span class="help">{{ taille(f.size) }}</span>
            <button
              type="button"
              class="dossier-retirer"
              :aria-label="`Retirer ${f.name}`"
              @click="retirer(i)"
            >
              ✕
            </button>
          </li>
        </ul>

        <Erreur :message="refuses" titre="Fichiers non ajoutés" fermable @fermer="refuses = []" />
      </fieldset>
    </div>

    <!-- À droite : le candidat -->
    <div class="dossier-colonne">
      <fieldset class="dossier-bloc">
        <legend>Candidat</legend>

        <div class="dossier-modes" role="radiogroup" aria-label="Origine du candidat">
          <label class="choice">
            <input v-model="mode" type="radio" value="existante" />
            Personne déjà dans la base
          </label>
          <label class="choice">
            <input v-model="mode" type="radio" value="nouvelle" />
            Nouvelle personne
          </label>
        </div>

        <!-- Personne de la base -->
        <template v-if="mode === 'existante'">
          <label for="dossier-personne">Rechercher une personne</label>
          <RechercheSelect
            id="dossier-personne"
            :options="options"
            placeholder="Nom, prénom…"
            vide="Personne ne correspond : ajoutez une nouvelle personne."
            @choisir="(o) => (idPersonne = o.id)"
          />
          <Erreur :message="erreurPersonnes" />

          <div v-if="personne" class="dossier-personne">
            <div class="dossier-personne-titre">
              <strong>{{ nomComplet(personne) }}</strong>
              <span class="badge" :class="personne.poste ? 'ok' : 'attente'">
                {{ personne.poste ? 'Employé(e)' : 'Externe' }}
              </span>
            </div>
            <p v-if="personne.poste" class="help">
              Candidature interne : occupe le poste « {{ personne.poste }} » ({{
                personne.departement
              }}).
            </p>
            <dl class="dossier-rappel">
              <dt>Naissance</dt>
              <dd>{{ dateLongue(personne.naissance) }}</dd>
              <dt>E-mail</dt>
              <dd>{{ personne.email }}</dd>
              <dt>Téléphone</dt>
              <dd>{{ personne.tel }}</dd>
              <dt>Adresse</dt>
              <dd>{{ personne.adresse }}</dd>
            </dl>
            <button type="button" class="dossier-lien" @click="idPersonne = null">
              Choisir une autre personne
            </button>
          </div>
          <p v-else class="dossier-aide">Aucun candidat choisi.</p>
        </template>

        <!-- Nouvelle personne (colonnes de la table personne) -->
        <template v-else>
          <div class="dossier-ligne">
            <div>
              <label for="nouvelle-nom">Nom</label>
              <input
                id="nouvelle-nom"
                v-model="nouvelle.nom"
                maxlength="100"
                required
                autocomplete="family-name"
              />
            </div>
            <div>
              <label for="nouvelle-prenom">Prénom</label>
              <input
                id="nouvelle-prenom"
                v-model="nouvelle.prenom"
                maxlength="100"
                required
                autocomplete="given-name"
              />
            </div>
          </div>

          <div class="dossier-ligne">
            <div>
              <label for="nouvelle-naissance">Date de naissance</label>
              <input
                id="nouvelle-naissance"
                v-model="nouvelle.naissance"
                type="date"
                :max="AUJOURDHUI"
                required
              />
            </div>
            <div>
              <label for="nouvelle-tel">Téléphone</label>
              <input
                id="nouvelle-tel"
                v-model="nouvelle.tel"
                type="tel"
                maxlength="20"
                required
                autocomplete="tel"
              />
            </div>
          </div>

          <label for="nouvelle-email">E-mail</label>
          <input
            id="nouvelle-email"
            v-model="nouvelle.email"
            type="email"
            maxlength="100"
            required
            autocomplete="email"
          />

          <label for="nouvelle-adresse">Adresse</label>
          <input
            id="nouvelle-adresse"
            v-model="nouvelle.adresse"
            maxlength="200"
            required
            autocomplete="street-address"
          />

          <Erreur v-if="doublon" titre="Cette personne existe déjà">
            {{ nomComplet(doublon) }} ({{ doublon.email }}) —
            <button type="button" class="dossier-lien" @click="choisirDoublon">
              la sélectionner
            </button>
          </Erreur>
        </template>
      </fieldset>

      <Erreur :message="erreur" fermable @fermer="erreur = ''" />
      <button type="submit">Déposer le dossier</button>

      <p v-if="envoye" class="success" role="status">
        Dossier de {{ envoye.candidat }} ({{ envoye.employe ? 'employé(e)' : 'externe'
        }}{{ envoye.nouvelle ? ', nouvelle personne' : '' }}) prêt pour l'offre «
        {{ envoye.offre }} », {{ envoye.fichiers }} fichier(s) — pas encore enregistré (aucun
        backend pour les dossiers).
      </p>
    </div>
  </form>
</template>
