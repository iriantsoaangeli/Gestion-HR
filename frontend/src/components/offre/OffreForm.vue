<script setup>
import { computed, reactive, ref, watch } from 'vue'
import Erreur from '../ui/Erreur.vue'
import DemandesColonne from '../demande/DemandesColonne.vue'
import * as api from '../../api/offres'
import { useChargement } from '../../utils/chargement'
import { lignesPostes, NATURES } from '../../utils/demande'
import { dateLongue, dansLe } from '../../utils/dates'

// Création d'une offre d'emploi (HR) à partir d'une demande déjà traitée et approuvée.
// Deux colonnes : l'offre à gauche, le tableau des demandes à droite.
// 1. Filtres (manager, tri, sens) au-dessus du tableau des demandes (même tableau que les pages de demandes).
//    Le tri est partagé avec le tableau : le changer ici ou par un en-tête revient au même.
//    Un clic sur une ligne choisit la demande, un second clic la désélectionne.
// 2. Le formulaire reste grisé et inutilisable tant qu'aucune demande n'est choisie.
// Données statiques (api/offres.js) : rien n'est encore envoyé au backend.

const { donnees: demandes, chargement, erreur: erreurDemandes } = useChargement(api.demandesApprouvees, [])
const { donnees: typesContrat } = useChargement(api.typesContrat, [])

// ---------- Filtres et tri ----------

// Clés = colonnes triables du tableau (components/demande/DemandesColonne.vue)
const TRIS = [
  { cle: 'date', label: 'Date de la demande' },
  { cle: 'delai', label: 'Délai' },
  { cle: 'id', label: 'N° de demande' },
  { cle: 'departement', label: 'Département' },
  { cle: 'nombre', label: 'Nombre de postes' },
]

const filtre = reactive({ manager: '' })
const tri = ref({ cle: 'date', sens: -1 }) // plus récentes d'abord ; lié au tableau (v-model:tri)

// Un choix par manager présent dans les demandes
const managers = computed(() => {
  const vus = new Map()
  demandes.value.forEach((d) => vus.set(d.demandeur.login, d.demandeur))
  return [...vus.values()].sort((a, b) => a.detenteur.localeCompare(b.detenteur, 'fr'))
})

// Le tri est appliqué par le tableau
const demandesFiltrees = computed(() =>
  demandes.value.filter((d) => !filtre.manager || d.demandeur.login === filtre.manager),
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
              <label for="offre-nombre">Postes</label>
              <input id="offre-nombre" :value="offre.nombre" type="number" readonly />
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

          <label class="choice choice-grande">
            <input v-model="offre.avecDateLimite" type="checkbox" />
            Ajouter une date limite de candidature
          </label>
          <template v-if="offre.avecDateLimite">
            <label for="offre-limite">Date limite de candidature</label>
            <input id="offre-limite" v-model="offre.dateLimite" type="date" :min="DATE_LIMITE_MIN" required />
          </template>

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
          <label for="filtre-manager">Manager</label>
          <select id="filtre-manager" v-model="filtre.manager">
            <option value="">Tous les managers</option>
            <option v-for="m in managers" :key="m.login" :value="m.login">{{ m.detenteur }} ({{ m.login }})</option>
          </select>
        </div>

        <div class="offre-filtre">
          <label for="filtre-tri">Trier par</label>
          <div class="offre-tri">
            <select id="filtre-tri" :value="tri.cle" @change="tri = { ...tri, cle: $event.target.value }">
              <option v-for="t in TRIS" :key="t.cle" :value="t.cle">{{ t.label }}</option>
              <!-- Tri choisi par un en-tête sans équivalent ici (postes, statut) -->
              <option v-if="!TRIS.some((t) => t.cle === tri.cle)" :value="tri.cle" disabled>Colonne du tableau</option>
            </select>
            <button
              type="button"
              class="offre-sens"
              :aria-label="tri.sens === 1 ? 'Ordre croissant (cliquer pour décroissant)' : 'Ordre décroissant (cliquer pour croissant)'"
              :title="tri.sens === 1 ? 'Croissant' : 'Décroissant'"
              @click="tri = { ...tri, sens: -tri.sens }"
            >
              {{ tri.sens === 1 ? '▲ Croissant' : '▼ Décroissant' }}
            </button>
          </div>
        </div>
      </div>

      <DemandesColonne
        titre="Demandes traitées (approuvées)"
        :demandes="demandesFiltrees"
        :vide="chargement ? 'Chargement…' : 'Aucune demande pour ce filtre'"
        v-model:selection="idDemande"
        v-model:tri="tri"
        :class="{ 'a-choisir': !idDemande }"
      />
      <p class="help">{{ demandesFiltrees.length }} demande(s) sur {{ demandes.length }}.</p>
      <Erreur :message="erreurDemandes" />

    </section>
  </div>
</template>
