<script setup>
import { computed, reactive, ref, watch } from 'vue'
import CaseOption from '../ui/CaseOption.vue'
import Erreur from '../ui/Erreur.vue'
import FiltrePuces from '../ui/FiltrePuces.vue'
import DemandesColonne from '../demande/DemandesColonne.vue'
import * as api from '../../api/offres'
import { useChargement } from '../../utils/chargement'
import { lignesPostes, NATURES } from '../../utils/demande'
import { dateLongue, dansLe } from '../../utils/dates'

// Création d'une offre d'emploi (HR) à partir d'une demande déjà traitée et approuvée.
// Deux colonnes : l'offre à gauche, le tableau des demandes à droite.
// 1. Puces de département et de manager au-dessus du tableau des demandes (même tableau que les pages
//    de demandes) ; le tri se fait en cliquant sur les en-têtes du tableau.
//    Un clic sur une ligne choisit la demande, un second clic la désélectionne.
// 2. Le formulaire reste grisé et inutilisable tant qu'aucune demande n'est choisie.
// 3. Une offre = une ligne de la demande ; une ligne déjà couverte par une offre est grisée dans la liste.
//    Une demande dont toutes les lignes ont déjà une offre n'est plus proposée (sauf si elle est choisie :
//    on garde le message de la dernière publication).
// Envoi : POST /api/offres (api/offres.js, doc/routes/api/offres.md).

const {
  donnees: demandes,
  chargement,
  erreur: erreurDemandes,
  recharger,
} = useChargement(api.demandesApprouvees, [])

// ---------- Filtres ----------

// '' = tous ; manager = login du demandeur (puces : components/ui/FiltrePuces.vue)
const filtre = reactive({ departement: '', manager: '' })

// Les managers proposés dépendent du département choisi (un manager = un département)
const demandesDuDepartement = computed(() =>
  demandes.value.filter((d) => !filtre.departement || d.departement === filtre.departement),
)
// Changer de département remet « Tous » si le manager choisi n'y est pas
watch(demandesDuDepartement, (liste) => {
  if (filtre.manager && !liste.some((d) => d.demandeur.login === filtre.manager))
    filtre.manager = ''
})

// Demande dont il reste au moins une ligne sans offre
const aPublier = (d) => lignesPostes(d).some((p) => !p.idOffre)

// Le tri est appliqué par le tableau (clic sur un en-tête)
const demandesFiltrees = computed(() =>
  demandes.value.filter(
    (d) =>
      (aPublier(d) || d.id === idDemande.value) &&
      (!filtre.departement || d.departement === filtre.departement) &&
      (!filtre.manager || d.demandeur.login === filtre.manager),
  ),
)
const nombreAPublier = computed(() => demandes.value.filter(aPublier).length)
// Texte du tableau vide
const texteVide = computed(() => {
  if (chargement.value) return 'Chargement…'
  if (!nombreAPublier.value) return 'Toutes les demandes approuvées ont déjà leurs offres'
  return 'Aucune demande pour ce filtre'
})

// ---------- Demande choisie ----------

const idDemande = ref(null)
const demande = computed(() => demandes.value.find((d) => d.id === idDemande.value) ?? null)
const postes = computed(() => (demande.value ? lignesPostes(demande.value) : []))
const postesLibres = computed(() => postes.value.filter((p) => !p.idOffre))

// Un filtre qui cache la demande choisie la désélectionne (le formulaire redevient grisé)
watch(demandesFiltrees, (liste) => {
  if (idDemande.value && !liste.some((d) => d.id === idDemande.value)) idDemande.value = null
})

// ---------- Offre ----------

const DELAI_MIN = dansLe({ jours: 1 })
const vide = () => ({
  poste: '',
  intitule: '',
  description: '',
  profil: '',
  avecDelai: false, // date limite de candidature (delai de l'API) facultative
  delai: dansLe({ mois: 1 }),
})
const offre = reactive(vide())
const publiee = ref(null) // offre renvoyée par l'API après publication
const erreur = ref('')
const envoi = ref(false)

// Nouvelle demande : on repart d'une offre vide, pré-remplie avec son premier poste encore libre
watch(idDemande, () => {
  Object.assign(offre, vide())
  publiee.value = null
  erreur.value = ''
  if (postesLibres.value.length) choisirPoste(postesLibres.value[0].cle)
})

function choisirPoste(cle) {
  const l = postes.value.find((p) => p.cle === cle)
  offre.poste = cle
  offre.intitule = l?.label ?? ''
}

// Affichage seul : nombre de postes à créer ou à remplacer pour le poste choisi
const texteNombre = computed(() => {
  const l = postes.value.find((p) => p.cle === offre.poste)
  if (!l) return '—'
  return l.nature === 'creation' ? `${l.effectif} à créer` : '1 à remplacer'
})

const libellePoste = (p) =>
  `${p.label}${p.effectif ? ` ×${p.effectif}` : ''} (${NATURES[p.nature].toLowerCase()})` +
  (p.idOffre ? ` — déjà publiée, offre n° ${p.idOffre}` : '')

// Premier problème du formulaire, '' si tout va bien (l = ligne choisie)
function verifier(l) {
  if (!l) return 'Toutes les lignes de cette demande ont déjà une offre.'
  if (!offre.intitule.trim()) return "L'intitulé de l'offre est obligatoire."
  if (!offre.description.trim()) return 'La description est obligatoire.'
  if (offre.avecDelai && offre.delai < DELAI_MIN)
    return 'La date limite de candidature doit être après aujourd’hui.'
  return ''
}

async function publier() {
  const l = postes.value.find((p) => p.cle === offre.poste)
  publiee.value = null
  erreur.value = verifier(l)
  if (erreur.value) return
  envoi.value = true
  try {
    const cree = await api.publier({
      idDemande: demande.value.id,
      idFonction: l.idFonction ?? null,
      idPoste: l.idPoste ?? null,
      intitule: offre.intitule.trim(),
      description: offre.description.trim(),
      profil: offre.profil.trim() || null,
      delai: offre.avecDelai ? offre.delai : null,
    })
    await recharger() // la ligne publiée passe en « déjà publiée »
    Object.assign(offre, vide())
    if (postesLibres.value.length) choisirPoste(postesLibres.value[0].cle)
    publiee.value = cree
  } catch (e) {
    erreur.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="offre">
    <!-- À gauche : rappel de la demande choisie, puis l'offre (grisée tant qu'aucune demande n'est choisie) -->
    <div class="offre-gauche">
      <!-- Rappel de la demande choisie -->
      <dl v-if="demande" class="offre-rappel">
        <dt>Manager</dt>
        <dd>{{ demande.demandeur.detenteur }} ({{ demande.demandeur.login }})</dd>
        <dt>Département</dt>
        <dd>{{ demande.departement }}</dd>
        <dt>Motif</dt>
        <dd>{{ demande.motif }}</dd>
        <dt>Délai</dt>
        <dd>{{ demande.delai ? dateLongue(demande.delai) : '—' }}</dd>
      </dl>

      <form class="form offre-form" @submit.prevent="publier">
        <p v-if="!demande" class="offre-aide">
          Choisissez d'abord une demande traitée dans le tableau à droite pour rédiger l'offre.
        </p>

        <fieldset :disabled="!demande">
          <legend>Offre d'emploi</legend>

          <label for="offre-poste">Poste à pourvoir</label>
          <select
            id="offre-poste"
            :value="offre.poste"
            :required="postesLibres.length > 0"
            @change="choisirPoste($event.target.value)"
          >
            <option v-for="p in postes" :key="p.cle" :value="p.cle" :disabled="Boolean(p.idOffre)">
              {{ libellePoste(p) }}
            </option>
          </select>
          <p v-if="demande && !postesLibres.length" class="help">
            Toutes les lignes de cette demande ont déjà une offre : choisissez une autre demande.
          </p>

          <div class="offre-ligne">
            <div>
              <label for="offre-intitule">Intitulé de l'offre</label>
              <input id="offre-intitule" v-model="offre.intitule" maxlength="150" required />
            </div>
            <div class="offre-court">
              <span class="offre-libelle">Postes</span>
              <p class="offre-nombre">{{ texteNombre }}</p>
            </div>
          </div>

          <label for="offre-description">Description</label>
          <textarea
            id="offre-description"
            v-model="offre.description"
            rows="4"
            maxlength="4000"
            required
          ></textarea>

          <label for="offre-profil">Profil recherché (facultatif)</label>
          <textarea id="offre-profil" v-model="offre.profil" rows="3" maxlength="4000"></textarea>

          <CaseOption v-model="offre.avecDelai" libelle="Ajouter une date limite de candidature">
            <label for="offre-limite">Date limite de candidature</label>
            <input id="offre-limite" v-model="offre.delai" type="date" :min="DELAI_MIN" required />
          </CaseOption>

          <Erreur :message="erreur" fermable @fermer="erreur = ''" />
          <button type="submit" :disabled="envoi || !postesLibres.length">
            {{ envoi ? 'Publication…' : "Publier l'offre" }}
          </button>
        </fieldset>

        <p v-if="publiee" class="success" role="status">
          Offre
          <RouterLink :to="`/offres/${publiee.id}`"
            >n° {{ publiee.id }} « {{ publiee.intitule }} »</RouterLink
          >
          publiée{{
            publiee.postes.length ? ` : ${publiee.postes.map((p) => p.code).join(', ')}` : ''
          }}.
        </p>
      </form>
    </div>

    <!-- À droite : choisir une demande déjà traitée -->
    <section class="offre-choix">
      <h2>Demande à l'origine de l'offre</h2>

      <div class="offre-filtres">
        <div class="offre-filtre">
          <span class="offre-filtre-titre">Département</span>
          <FiltrePuces
            v-model="filtre.departement"
            :elements="demandes"
            aria-label="Département de la demande"
          />
        </div>
        <div class="offre-filtre">
          <span class="offre-filtre-titre">Manager</span>
          <FiltrePuces
            v-model="filtre.manager"
            :elements="demandesDuDepartement"
            :code="(d) => d.demandeur.login"
            :libelle="(d) => d.demandeur.detenteur"
            aria-label="Manager qui a fait la demande"
          />
        </div>
      </div>

      <DemandesColonne
        titre="Demandes traitées (approuvées)"
        :demandes="demandesFiltrees"
        :vide="texteVide"
        :masquer="['statut']"
        v-model:selection="idDemande"
        :puces-statut="false"
        :class="{ 'a-choisir': !idDemande }"
      />
      <p class="help">
        {{ demandesFiltrees.length }} demande(s) sur {{ nombreAPublier }} avec des postes encore
        sans offre.
      </p>
      <Erreur :message="erreurDemandes" />
    </section>
  </div>
</template>
