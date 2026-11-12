<script setup>
import { computed, reactive, ref, watch } from 'vue'
import Erreur from '../ui/Erreur.vue'
import Tableau from '../ui/Tableau.vue'
import * as apiDossiers from '../../api/dossiers'
import * as apiEntretiens from '../../api/entretiens'
import { useChargement } from '../../utils/chargement'
import { dateLongue } from '../../utils/dates'
import { contient } from '../../utils/texte'

// Préparation d'un entretien (HR) : une offre ouverte, un candidat (dossier déposé pour cette offre),
// la date et l'heure, puis qui fait l'entretien (un ou plusieurs postes).
// À gauche : l'offre puis le candidat (grisé tant qu'aucune offre n'est choisie).
// À droite : la date, les interviewers, le bouton et le récapitulatif.
// offre / dossier : choix déjà faits (query ?offre=…&dossier=…, ex. depuis le détail d'une offre).
// Données statiques (api/dossiers.js, api/entretiens.js) : l'entretien reste en mémoire.
const props = defineProps({
  offre: { type: [String, Number], default: null },
  dossier: { type: [String, Number], default: null },
})

const { donnees: offres, erreur: erreurOffres } = useChargement(apiDossiers.offresOuvertes, [])
const { donnees: personnes } = useChargement(apiDossiers.personnes, []) // département des candidats employés
const { donnees: interviewers, erreur: erreurInterviewers } = useChargement(
  apiEntretiens.interviewersPossibles,
  [],
)

const dateCourte = (texte) =>
  texte ? new Date(`${texte.slice(0, 10)}T00:00:00`).toLocaleDateString('fr-FR') : null
// « 18/11/2026 09:00 »
const dateHeure = (texte) =>
  new Date(texte).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

// Départements présents dans une liste (puces au-dessus des tableaux)
const departementsDe = (liste) =>
  [...new Set(liste.map((x) => x.departement).filter(Boolean))].sort((a, b) =>
    a.localeCompare(b, 'fr'),
  )

// Filtre d'un tableau : recherche (sans accents) + département ('' = tous)
function filtrer(liste, filtre, texte) {
  return liste.filter(
    (x) =>
      (!filtre.departement || x.departement === filtre.departement) &&
      contient(texte(x), filtre.recherche),
  )
}

// ---------- Offre ----------

const idOffre = ref(null)
const offre = computed(() => offres.value.find((o) => o.id === idOffre.value) ?? null)

const COLONNES_OFFRE = [
  { cle: 'id', label: 'N°', classe: 'nombre' },
  { cle: 'poste', label: 'Poste' },
  { cle: 'departement', label: 'Département' },
  { cle: 'responsable', label: 'Responsable' },
  {
    cle: 'delai',
    label: 'Date limite',
    classe: 'etroite',
    valeur: (o) => dateCourte(o.delai),
    valeurTri: (o) => o.delai || '9999', // sans date limite = en dernier
  },
]
const filtreOffre = reactive({ recherche: '', departement: '' })
const departementsOffres = computed(() => departementsDe(offres.value))
const nombreOffres = (d) => offres.value.filter((o) => !d || o.departement === d).length
const offresFiltrees = computed(() =>
  filtrer(offres.value, filtreOffre, (o) =>
    [o.id, o.poste, o.departement, o.responsable, o.description].join(' '),
  ),
)

// ---------- Candidat (dossier déposé pour l'offre) ----------

const idDossier = ref(null)
const dossiersOffre = ref([])
const entretiensOffre = ref([]) // pour signaler un candidat qui a déjà un entretien
const erreurDossiers = ref('')

// Dossiers + département du candidat (celui de son poste s'il est employé, aucun s'il est externe)
const candidats = computed(() =>
  dossiersOffre.value.map((d) => ({
    ...d,
    departement: personnes.value.find((p) => p.id === d.idPersonne)?.departement ?? null,
    entretiens: entretiensOffre.value.filter((e) => e.idDossier === d.id),
  })),
)
const candidat = computed(() => candidats.value.find((c) => c.id === idDossier.value) ?? null)

const COLONNES_CANDIDAT = [
  { cle: 'candidat', label: 'Candidat' },
  {
    cle: 'statut',
    label: 'Statut',
    classe: 'etroite',
    valeur: (c) => (c.employe ? 'Employé(e)' : 'Externe'),
  },
  {
    cle: 'date',
    label: 'Dépôt',
    classe: 'etroite',
    valeur: (c) => dateCourte(c.date),
    valeurTri: (c) => c.date,
  },
  {
    cle: 'entretien',
    label: 'Entretien',
    classe: 'large',
    valeur: (c) => (c.entretiens.length ? dateHeure(c.entretiens.at(-1).date) : 'Aucun'),
    valeurTri: (c) => c.entretiens.at(-1)?.date ?? '',
  },
]
const filtreCandidat = reactive({ recherche: '', departement: '' })
const departementsCandidats = computed(() => departementsDe(candidats.value))
const nombreCandidats = (d) => candidats.value.filter((c) => !d || c.departement === d).length
const candidatsFiltres = computed(() =>
  filtrer(candidats.value, filtreCandidat, (c) =>
    [c.candidat, c.email, c.tel, c.departement, ...c.fichiers].join(' '),
  ),
)

// Nouvelle offre : on recharge ses dossiers et ses entretiens ; le candidat choisi est gardé
// seulement s'il a déposé pour cette offre (sert aussi au candidat reçu en query)
async function chargerOffre(id, dossierVoulu = null) {
  dossiersOffre.value = []
  entretiensOffre.value = []
  erreurDossiers.value = ''
  if (!id) return (idDossier.value = null)
  try {
    const [d, e] = await Promise.all([
      apiDossiers.dossiersDeLOffre(id),
      apiEntretiens.entretiensDeLOffre(id),
    ])
    dossiersOffre.value = d
    entretiensOffre.value = e
  } catch (e) {
    erreurDossiers.value = e.message
  }
  idDossier.value = dossiersOffre.value.some((d) => d.id === dossierVoulu) ? dossierVoulu : null
}
watch(idOffre, (id) => chargerOffre(id, idDossier.value))

// Choix reçus en query : l'offre (si elle est ouverte), le candidat (s'il a déposé pour cette offre).
// Un dossier seul suffit : son offre est retrouvée.
watch(
  offres,
  async (liste) => {
    if (!liste.length || (!props.offre && !props.dossier)) return
    const voulu = props.dossier ? Number(props.dossier) : null
    let id = props.offre ? Number(props.offre) : null
    if (!id && voulu) id = (await apiDossiers.dossiers()).find((d) => d.id === voulu)?.idOffre
    if (!liste.some((o) => o.id === id)) return
    idDossier.value = voulu
    idOffre.value = id
  },
  { once: true },
)

// ---------- Date ----------

const date = ref('') // 'AAAA-MM-JJTHH:MM' (champ datetime-local, heure locale)
const maintenant = () => {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset()) // heure locale au format ISO
  return d.toISOString().slice(0, 16)
}
const minimum = maintenant()

// ---------- Interviewers ----------

const choisis = ref([]) // idPoste des interviewers retenus
const COLONNES_INTERVIEWER = [
  { cle: 'choisi', label: '', classe: 'case', triable: false },
  { cle: 'poste', label: 'Poste' },
  { cle: 'detenteur', label: 'Tenu par' },
  { cle: 'role', label: '', classe: 'large', triable: false },
]
// interviewersPossibles() ne renvoie pas encore le département : les puces se limitent alors à « Tous »
const filtreInterviewer = reactive({ recherche: '', departement: '' })
const departementsInterviewers = computed(() => departementsDe(interviewers.value))
const nombreInterviewers = (d) => interviewers.value.filter((i) => !d || i.departement === d).length
const interviewersFiltres = computed(() =>
  filtrer(interviewers.value, filtreInterviewer, (i) =>
    [i.poste, i.detenteur, i.login, i.departement].join(' '),
  ),
)
const basculer = (i) =>
  (choisis.value = choisis.value.includes(i.idPoste)
    ? choisis.value.filter((id) => id !== i.idPoste)
    : [...choisis.value, i.idPoste])
const estResponsable = (i) => offre.value?.idPosteResponsable === i.idPoste
const nomsChoisis = computed(() =>
  interviewers.value.filter((i) => choisis.value.includes(i.idPoste)).map((i) => i.poste),
)

// ---------- Envoi ----------

const erreur = ref('')
const envoi = ref(false)
const prepare = ref(null) // entretien créé (récapitulatif)

async function valider() {
  erreur.value = !offre.value
    ? 'Choisissez une offre.'
    : !candidat.value
      ? 'Choisissez un candidat (un dossier déposé pour cette offre).'
      : !date.value
        ? "Indiquez la date et l'heure de l'entretien."
        : date.value <= maintenant()
          ? "La date de l'entretien doit être dans le futur."
          : !choisis.value.length
            ? "Choisissez au moins une personne qui fait l'entretien."
            : ''
  if (erreur.value) return

  envoi.value = true
  try {
    prepare.value = await apiEntretiens.preparer({
      idDossier: candidat.value.id,
      date: `${date.value}:00`,
      interviewers: [...choisis.value],
    })
    // On garde l'offre (plusieurs candidats à convoquer) ; le nouvel entretien apparaît dans le tableau
    date.value = ''
    choisis.value = []
    await chargerOffre(idOffre.value)
  } catch (e) {
    erreur.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <form class="preparation" @submit.prevent="valider">
    <!-- À gauche : l'offre puis le candidat -->
    <div class="preparation-colonne">
      <fieldset class="preparation-bloc">
        <legend>1. Offre</legend>
        <p class="preparation-titre-liste">
          Offre ouverte <span class="help">— cliquez sur une ligne</span>
        </p>
        <div class="filtres preparation-filtres">
          <div class="puces" role="group" aria-label="Département de l'offre">
            <button
              v-for="d in ['', ...departementsOffres]"
              :key="d"
              type="button"
              class="puce"
              :class="{ active: filtreOffre.departement === d }"
              :aria-pressed="filtreOffre.departement === d"
              @click="filtreOffre.departement = d"
            >
              {{ d || 'Tous' }} <span class="count">{{ nombreOffres(d) }}</span>
            </button>
          </div>
          <input
            v-model="filtreOffre.recherche"
            type="search"
            class="filtre-recherche"
            placeholder="Rechercher (n°, poste, responsable, description)…"
            aria-label="Rechercher une offre"
          />
        </div>
        <Tableau
          v-model:selection="idOffre"
          :lignes="offresFiltrees"
          :colonnes="COLONNES_OFFRE"
          :titre-ligne="(o) => o.description"
          :vide="offres.length ? 'Aucune offre pour ce filtre' : 'Aucune offre ouverte'"
          :class="{ 'a-choisir': !idOffre }"
        />
        <Erreur :message="erreurOffres" />
        <p v-if="offre" class="preparation-rappel">
          <strong>N° {{ offre.id }} — {{ offre.poste }}</strong> ({{ offre.departement }}) ·
          responsable : {{ offre.responsable }} · date limite :
          {{ offre.delai ? dateLongue(offre.delai) : 'aucune' }}
        </p>
      </fieldset>

      <fieldset class="preparation-bloc" :disabled="!offre" :class="{ grise: !offre }">
        <legend>2. Candidat</legend>
        <p class="preparation-titre-liste">
          Dossier déposé pour l'offre
          <span class="help"
            >— {{ offre ? 'cliquez sur une ligne' : "choisissez d'abord une offre" }}</span
          >
        </p>
        <div class="filtres preparation-filtres">
          <div class="puces" role="group" aria-label="Département du candidat">
            <button
              v-for="d in ['', ...departementsCandidats]"
              :key="d"
              type="button"
              class="puce"
              :class="{ active: filtreCandidat.departement === d }"
              :aria-pressed="filtreCandidat.departement === d"
              @click="filtreCandidat.departement = d"
            >
              {{ d || 'Tous' }} <span class="count">{{ nombreCandidats(d) }}</span>
            </button>
          </div>
          <input
            v-model="filtreCandidat.recherche"
            type="search"
            class="filtre-recherche"
            placeholder="Rechercher (nom, e-mail, téléphone, fichier)…"
            aria-label="Rechercher un candidat"
          />
        </div>
        <Tableau
          v-model:selection="idDossier"
          :lignes="candidatsFiltres"
          :colonnes="COLONNES_CANDIDAT"
          :titre-ligne="(c) => `${c.email} · ${c.tel} · ${c.fichiers.join(', ')}`"
          :vide="
            !offre
              ? 'Aucune offre choisie'
              : candidats.length
                ? 'Aucun candidat pour ce filtre'
                : 'Aucun dossier déposé pour cette offre'
          "
          :class="{ 'a-choisir': offre && !idDossier }"
        >
          <template #statut="{ ligne, valeur }">
            <span class="badge petit" :class="ligne.employe ? 'ok' : 'attente'">{{ valeur }}</span>
          </template>
          <template #entretien="{ ligne, valeur }">
            <span v-if="ligne.entretiens.length" class="badge petit attente">{{ valeur }}</span>
            <span v-else class="help">{{ valeur }}</span>
          </template>
        </Tableau>
        <Erreur :message="erreurDossiers" />
        <p v-if="candidat?.entretiens.length" class="preparation-aide">
          {{ candidat.candidat }} a déjà {{ candidat.entretiens.length }} entretien(s) pour cette
          offre (le {{ dateHeure(candidat.entretiens.at(-1).date) }}) : vous pouvez en prévoir un
          autre.
        </p>
      </fieldset>
    </div>

    <!-- À droite : la date, les interviewers, l'envoi -->
    <div class="preparation-colonne">
      <fieldset class="preparation-bloc">
        <legend>3. Date et heure</legend>
        <label for="preparation-date">Date et heure de l'entretien</label>
        <input id="preparation-date" v-model="date" type="datetime-local" :min="minimum" required />
      </fieldset>

      <fieldset class="preparation-bloc">
        <legend>4. Qui fait l'entretien</legend>
        <p class="preparation-titre-liste">
          Interviewers <span class="help">— un ou plusieurs, cliquez pour cocher</span>
        </p>
        <div class="filtres preparation-filtres">
          <div class="puces" role="group" aria-label="Département de l'interviewer">
            <button
              v-for="d in ['', ...departementsInterviewers]"
              :key="d"
              type="button"
              class="puce"
              :class="{ active: filtreInterviewer.departement === d }"
              :aria-pressed="filtreInterviewer.departement === d"
              @click="filtreInterviewer.departement = d"
            >
              {{ d || 'Tous' }} <span class="count">{{ nombreInterviewers(d) }}</span>
            </button>
          </div>
          <input
            v-model="filtreInterviewer.recherche"
            type="search"
            class="filtre-recherche"
            placeholder="Rechercher (poste, nom)…"
            aria-label="Rechercher un interviewer"
          />
        </div>
        <Tableau
          :lignes="interviewersFiltres"
          :colonnes="COLONNES_INTERVIEWER"
          cle-ligne="idPoste"
          cliquable
          vide="Aucun interviewer pour ce filtre"
          :class="{ 'a-choisir': !choisis.length }"
          @ouvrir="basculer"
        >
          <!-- Case d'affichage : le clic sur la ligne coche / décoche -->
          <template #choisi="{ ligne }">
            <input
              type="checkbox"
              :checked="choisis.includes(ligne.idPoste)"
              :aria-label="`Choisir ${ligne.poste}`"
              tabindex="-1"
              @click.prevent
            />
          </template>
          <template #role="{ ligne }">
            <span v-if="estResponsable(ligne)" class="badge petit ok" title="Responsable de l'offre"
              >Responsable</span
            >
            <span v-else></span>
          </template>
        </Tableau>
        <Erreur :message="erreurInterviewers" />
        <p class="help preparation-compte">
          {{
            choisis.length ? `Choisi(s) : ${nomsChoisis.join(', ')}` : 'Aucun interviewer choisi.'
          }}
        </p>
      </fieldset>

      <Erreur :message="erreur" fermable @fermer="erreur = ''" />
      <button type="submit" :disabled="envoi">Préparer l'entretien</button>

      <div v-if="prepare" class="success preparation-recap" role="status">
        Entretien N° {{ prepare.id }} prévu le {{ dateHeure(prepare.date) }} :
        {{ prepare.candidat }} pour « {{ prepare.poste }} » ({{ prepare.departement }}), avec
        {{ prepare.interviewers.map((i) => i.poste).join(', ') }}.
        <RouterLink :to="`/entretiens/${prepare.id}`">Voir l'entretien</RouterLink>
      </div>
    </div>
  </form>
</template>
