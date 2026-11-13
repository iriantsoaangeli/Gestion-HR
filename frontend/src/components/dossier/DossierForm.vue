<script setup>
import { computed, reactive, ref, watch } from 'vue'
import Erreur from '../ui/Erreur.vue'
import Tableau from '../ui/Tableau.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import BarreRecherche from '../ui/BarreRecherche.vue'
import * as api from '../../api/dossiers'
import { useChargement } from '../../utils/chargement'
import { dateCourte, dateLongue, iso } from '../../utils/dates'
import { contient, normaliser } from '../../utils/texte'

// Dépôt d'un dossier de candidature (HR) : une offre ouverte, un candidat, des pièces jointes.
// À gauche : l'offre, puis les fichiers (30 Mo au total). À droite : le candidat,
// choisi dans la base (employé ou externe) ou ajouté avec le formulaire « Nouvelle personne ».
// Envoi : POST /api/dossiers en multipart (api/dossiers.js, doc/routes/api/dossiers.md).

// offre : id d'une offre à choisir d'avance (si elle est ouverte)
const props = defineProps({ offre: { type: [String, Number], default: null } })

const TAILLE_MAX = 30 * 1024 * 1024 // 30 Mo pour tout le dossier
const AUJOURDHUI = iso(new Date())

const {
  donnees: personnes,
  erreur: erreurPersonnes,
  recharger: rechargerPersonnes,
} = useChargement(api.personnes, [])
const { donnees: offres, erreur: erreurOffres } = useChargement(api.offresOuvertes, [])

// ---------- Offre ----------

const idOffre = ref(null)
const offre = computed(() => offres.value.find((o) => o.id === idOffre.value) ?? null)
// Offre reçue en query : choisie dès que la liste des offres ouvertes arrive
watch(
  offres,
  (liste) => {
    const voulue = Number(props.offre)
    if (liste.some((o) => o.id === voulue)) idOffre.value = voulue
  },
  { once: true },
)

// Colonnes de la table offre (libellés à la place des id) ; la description s'affiche au survol
const COLONNES_OFFRE = [
  { cle: 'id', label: 'N°', classe: 'nombre' },
  { cle: 'intitule', label: 'Poste' },
  { cle: 'departement', label: 'Département' },
  { cle: 'responsable', label: 'Responsable' },
  {
    cle: 'delai',
    label: 'Date limite',
    classe: 'etroite',
    valeur: (o) => dateCourte(o.delai),
    valeurTri: (o) => o.delai || '9999',
  }, // sans date limite = en dernier
  {
    cle: 'idDemande',
    label: 'Demande',
    classe: 'tres-etroite',
    valeur: (o) => `N° ${o.idDemande}`,
  },
]
const libelleOffre = (o) => `N° ${o.id} — ${o.intitule} (${o.departement})`

// Filtre au-dessus du tableau : recherche (sans accents, utils/texte.js) + département
const filtreOffre = reactive({ recherche: '', departement: '' }) // '' = tous les départements
const offresFiltrees = computed(() =>
  offres.value.filter(
    (o) =>
      (!filtreOffre.departement || o.departement === filtreOffre.departement) &&
      contient(
        [o.id, o.intitule, o.departement, o.responsable, o.description].join(' '),
        filtreOffre.recherche,
      ),
  ),
)

// ---------- Candidat ----------

const mode = ref('existante') // 'existante' : choisi dans la base ; 'nouvelle' : ajouté ici
const idPersonne = ref(null)
const personne = computed(() => personnes.value.find((p) => p.id === idPersonne.value) ?? null)

const nomComplet = (p) => `${p.prenom} ${p.nom}`
const statutPersonne = (p) => (p.poste ? 'Employé(e)' : 'Externe') // employé = occupe un poste (table emploi)

// Colonnes de la table personne + son poste (table emploi) ; e-mail et téléphone au survol
const COLONNES_PERSONNE = [
  { cle: 'nom', label: 'Nom' },
  { cle: 'prenom', label: 'Prénom' },
  {
    cle: 'naissance',
    label: 'Naissance',
    classe: 'etroite',
    valeur: (p) => dateCourte(p.naissance),
    valeurTri: (p) => p.naissance,
  },
  { cle: 'statut', label: 'Statut', classe: 'etroite', valeur: statutPersonne },
  { cle: 'poste', label: 'Poste' },
]
// Filtre au-dessus du tableau : recherche + département (celui du poste ; les externes n'en ont pas)
const filtrePersonne = reactive({ recherche: '', departement: '' }) // '' = tous les départements
const personnesFiltrees = computed(() =>
  personnes.value.filter(
    (p) =>
      (!filtrePersonne.departement || p.departement === filtrePersonne.departement) &&
      contient([p.nom, p.prenom, p.poste, p.email, p.tel].join(' '), filtrePersonne.recherche),
  ),
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

function taille(octets) {
  if (octets === 0) return '0 Ko'
  if (octets < 1024 * 1024) return `${Math.max(1, Math.round(octets / 1024))} Ko`
  return `${(octets / 1024 / 1024).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} Mo`
}

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
const envoye = ref(null) // dossier renvoyé par l'API
const envoi = ref(false)

// Premier problème du formulaire, '' si tout va bien
function verifier() {
  if (!offre.value) return 'Choisissez une offre.'
  if (mode.value === 'existante' && !personne.value)
    return 'Choisissez un candidat dans la base, ou ajoutez une nouvelle personne.'
  if (doublon.value)
    return `Cette personne existe déjà (${nomComplet(doublon.value)}) : sélectionnez-la au lieu de la recréer.`
  if (!fichiers.value.length) return 'Joignez au moins un fichier (CV, lettre de motivation…).'
  return ''
}

// Une erreur affichée disparaît dès qu'on corrige le formulaire
watch(
  [idOffre, idPersonne, mode, () => fichiers.value.length, nouvelle],
  () => (erreur.value = ''),
  {
    deep: true,
  },
)

async function deposer() {
  envoye.value = null // pas de vieux message de succès à côté d'une nouvelle erreur
  erreur.value = verifier()
  if (erreur.value) return

  const candidat =
    mode.value === 'existante'
      ? { idPersonne: personne.value.id }
      : Object.fromEntries(Object.entries(nouvelle).map(([k, v]) => [k, v.trim()]))
  envoi.value = true
  try {
    const d = await api.deposer(offre.value.id, candidat, fichiers.value)
    envoye.value = { ...d, offre: libelleOffre(offre.value), nouvelle: mode.value === 'nouvelle' }
    if (mode.value === 'nouvelle') {
      await rechargerPersonnes() // la nouvelle personne est maintenant dans la base
      mode.value = 'existante'
    }
    idPersonne.value = null
    Object.assign(nouvelle, vide())
    fichiers.value = []
    refuses.value = []
  } catch (e) {
    erreur.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <form class="dossier" @submit.prevent="deposer">
    <!-- À gauche : l'offre et les pièces jointes -->
    <div class="dossier-colonne">
      <fieldset class="dossier-bloc">
        <legend>Offre</legend>
        <p class="dossier-titre-liste">
          Offre ouverte <span class="help">— cliquez sur une ligne</span>
        </p>
        <div class="filtres dossier-filtres">
          <FiltrePuces
            v-model="filtreOffre.departement"
            :elements="offres"
            aria-label="Département de l'offre"
          />
          <BarreRecherche
            v-model="filtreOffre.recherche"
            placeholder="Rechercher (n°, poste, responsable, description)…"
            aria-label="Rechercher une offre"
          />
        </div>
        <Tableau
          infobulles
          v-model:selection="idOffre"
          :lignes="offresFiltrees"
          :colonnes="COLONNES_OFFRE"
          :titre-ligne="(o) => o.description"
          :vide="offres.length ? 'Aucune offre pour ce filtre' : 'Aucune offre ouverte'"
          :class="{ 'a-choisir': !idOffre }"
        />
        <p class="help dossier-compte">
          {{ offresFiltrees.length }} offre(s) sur {{ offres.length }}
        </p>
        <Erreur :message="erreurOffres" />

        <dl v-if="offre" class="dossier-rappel">
          <dt>Poste</dt>
          <dd>{{ offre.intitule }}</dd>
          <dt>Département</dt>
          <dd>{{ offre.departement }}</dd>
          <dt>Responsable</dt>
          <dd>{{ offre.responsable }}</dd>
          <dt>Date limite</dt>
          <dd>{{ offre.delai ? dateLongue(offre.delai) : 'Aucune' }}</dd>
          <dt>Demande</dt>
          <dd>N° {{ offre.idDemande }}</dd>
          <dt>Description</dt>
          <dd>{{ offre.description }}</dd>
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

        <Erreur
          v-if="refuses.length"
          :message="refuses"
          titre="Fichiers non ajoutés"
          fermable
          @fermer="refuses = []"
        />
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
          <p class="dossier-titre-liste">
            Personne <span class="help">— cliquez sur une ligne</span>
          </p>
          <div class="filtres dossier-filtres">
            <FiltrePuces
              v-model="filtrePersonne.departement"
              :elements="personnes"
              aria-label="Département de la personne"
            />
            <BarreRecherche
              v-model="filtrePersonne.recherche"
              placeholder="Rechercher (nom, prénom, poste, e-mail, téléphone)…"
              aria-label="Rechercher une personne"
            />
          </div>
          <Tableau
            infobulles
            v-model:selection="idPersonne"
            :lignes="personnesFiltrees"
            :colonnes="COLONNES_PERSONNE"
            :titre-ligne="(p) => `${p.email} · ${p.tel}`"
            vide="Personne ne correspond : ajoutez une nouvelle personne"
            :class="{ 'a-choisir': !idPersonne }"
          >
            <template #statut="{ ligne, valeur }">
              <span class="badge petit" :class="ligne.poste ? 'ok' : 'attente'">{{ valeur }}</span>
            </template>
          </Tableau>
          <p class="help dossier-compte">
            {{ personnesFiltrees.length }} personne(s) sur {{ personnes.length }}
          </p>
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
      <button type="submit" :disabled="envoi">
        {{ envoi ? 'Envoi…' : 'Déposer le dossier' }}
      </button>

      <p v-if="envoye" class="success" role="status">
        Dossier n° {{ envoye.id }} de {{ envoye.candidat }} ({{
          envoye.employe ? 'employé(e)' : 'externe'
        }}{{ envoye.nouvelle ? ', nouvelle personne' : '' }}) déposé pour l'offre «
        {{ envoye.offre }} », {{ envoye.fichiers.length }} fichier(s).
      </p>
    </div>
  </form>
</template>
