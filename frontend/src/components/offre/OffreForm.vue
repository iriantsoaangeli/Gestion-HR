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
// Données statiques (api/offres.js) : rien n'est encore envoyé au backend.

const { donnees: demandes, chargement, erreur: erreurDemandes } = useChargement(api.demandesApprouvees, [])
const { donnees: typesContrat } = useChargement(api.typesContrat, [])

// ---------- Filtres ----------

// '' = tous ; manager = login du demandeur (puces : components/ui/FiltrePuces.vue)
const filtre = reactive({ departement: '', manager: '' })

// Les managers proposés dépendent du département choisi (un manager = un département)
const demandesDuDepartement = computed(() =>
  demandes.value.filter((d) => !filtre.departement || d.departement === filtre.departement),
)
// Changer de département remet « Tous » si le manager choisi n'y est pas
watch(demandesDuDepartement, (liste) => {
  if (filtre.manager && !liste.some((d) => d.demandeur.login === filtre.manager)) filtre.manager = ''
})

// Le tri est appliqué par le tableau (clic sur un en-tête)
const demandesFiltrees = computed(() =>
  demandes.value.filter(
    (d) =>
      (!filtre.departement || d.departement === filtre.departement) &&
      (!filtre.manager || d.demandeur.login === filtre.manager),
  ),
)

// ---------- Demande choisie ----------

const idDemande = ref(null)
const demande = computed(() => demandes.value.find((d) => d.id === idDemande.value) ?? null)
const postes = computed(() => (demande.value ? lignesPostes(demande.value) : []))

// Un filtre qui cache la demande choisie la désélectionne (le formulaire redevient grisé)
watch(demandesFiltrees, (liste) => {
  if (idDemande.value && !liste.some((d) => d.id === idDemande.value)) idDemande.value = null
})

// ---------- Offre ----------

const DATE_LIMITE_MIN = dansLe({ jours: 1 })
const vide = () => ({
  poste: '',
  intitule: '',
  nombre: 1,
  contrat: 'CDI',
  dureeMois: 6,
  profil: '',
  avecDateLimite: false, // date limite de candidature facultative
  dateLimite: dansLe({ mois: 1 }),
})
const offre = reactive(vide())
const envoyee = ref(null)
const erreur = ref('')

// Nouvelle demande : on repart d'une offre vide, pré-remplie avec son premier poste
watch(idDemande, () => {
  Object.assign(offre, vide())
  envoyee.value = null
  erreur.value = ''
  if (postes.value.length) choisirPoste(postes.value[0].cle)
})

function choisirPoste(cle) {
  const l = postes.value.find((p) => p.cle === cle)
  offre.poste = cle
  offre.intitule = l?.label ?? ''
  offre.nombre = l?.effectif ?? 1 // remplacement : 1 poste = 1 siège
}

// Affichage seul : nombre de postes à créer ou à remplacer pour le poste choisi
const texteNombre = computed(() => {
  const l = postes.value.find((p) => p.cle === offre.poste)
  if (!l) return '—'
  const n = offre.nombre
  return `${n} à ${l.nature === 'creation' ? 'créer' : 'remplacer'}`
})

const avecDuree = computed(() => offre.contrat !== 'CDI')

function publier() {
  erreur.value = !offre.intitule.trim()
    ? "L'intitulé de l'offre est obligatoire."
    : offre.avecDateLimite && offre.dateLimite < DATE_LIMITE_MIN
      ? 'La date limite de candidature doit être après aujourd’hui.'
      : ''
  if (erreur.value) return
  // Pas encore de backend : on affiche seulement le récapitulatif
  const { avecDateLimite, ...donnees } = offre
  envoyee.value = { ...donnees, dateLimite: avecDateLimite ? offre.dateLimite : null, demande: demande.value.id }
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
        <p v-if="!demande" class="offre-aide">Choisissez d'abord une demande traitée dans le tableau à droite pour rédiger l'offre.</p>

        <fieldset :disabled="!demande">
          <legend>Offre d'emploi</legend>

          <label for="offre-poste">Poste à pourvoir</label>
          <select id="offre-poste" :value="offre.poste" required @change="choisirPoste($event.target.value)">
            <option v-for="p in postes" :key="p.cle" :value="p.cle">
              {{ p.label }}{{ p.effectif ? ` ×${p.effectif}` : '' }} ({{ NATURES[p.nature].toLowerCase() }})
            </option>
          </select>

          <div class="offre-ligne">
            <div>
              <label for="offre-intitule">Intitulé de l'offre</label>
              <input id="offre-intitule" v-model="offre.intitule" maxlength="100" required />
            </div>
            <div class="offre-court">
              <span class="offre-libelle">Postes</span>
              <p class="offre-nombre">{{ texteNombre }}</p>
            </div>
          </div>

          <div class="offre-ligne">
            <div>
              <label for="offre-contrat">Type de contrat</label>
              <select id="offre-contrat" v-model="offre.contrat">
                <option v-for="t in typesContrat" :key="t.id" :value="t.id">{{ t.label }}</option>
              </select>
            </div>
            <div v-if="avecDuree" class="offre-court">
              <label for="offre-duree">Durée (mois)</label>
              <input id="offre-duree" v-model.number="offre.dureeMois" type="number" min="1" max="36" required />
            </div>
          </div>

          <label for="offre-profil">Profil recherché</label>
          <textarea id="offre-profil" v-model="offre.profil" rows="3" maxlength="1000"></textarea>

          <CaseOption v-model="offre.avecDateLimite" libelle="Ajouter une date limite de candidature">
            <label for="offre-limite">Date limite de candidature</label>
            <input id="offre-limite" v-model="offre.dateLimite" type="date" :min="DATE_LIMITE_MIN" required />
          </CaseOption>

          <Erreur :message="erreur" fermable @fermer="erreur = ''" />
          <button type="submit">Publier l'offre</button>
        </fieldset>

        <p v-if="envoyee" class="success" role="status">
          Offre « {{ envoyee.intitule }} » prête pour la demande n° {{ envoyee.demande }} — pas encore enregistrée
          (aucun backend pour les offres).
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
        :vide="chargement ? 'Chargement…' : 'Aucune demande pour ce filtre'"
        :masquer="['statut']"
        v-model:selection="idDemande"
        :puces-statut="false"
        :class="{ 'a-choisir': !idDemande }"
      />
      <p class="help">{{ demandesFiltrees.length }} demande(s) sur {{ demandes.length }}.</p>
      <Erreur :message="erreurDemandes" />

    </section>
  </div>
</template>
