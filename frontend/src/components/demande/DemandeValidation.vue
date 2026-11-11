<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FicheDemande from './FicheDemande.vue'
import { utilisateur } from '../../auth/session'
import Progression from '../ui/Progression.vue'
import { demandes, trouverDemande, aujourdhui, labelDepartement, totalPostes } from '../../data/demandes'
import { etapeSuivante, labelEtape } from '../../data/etapes'

const props = defineProps({ etape: { type: String, required: true } })
const route = useRoute()
const router = useRouter()

// Demande choisie (?id=), sinon la première en attente à cette étape.
// idDecide garde la demande affichée après la décision (sinon on passerait à la suivante sans le voir)
const idDecide = ref(null)
const d = computed(() => {
  const id = Number(route.query.id) || idDecide.value
  return id ? trouverDemande(id) : demandes.value.find((x) => x.etape === props.etape)
})

// Autres demandes qui attendent cette étape (les plus anciennes d'abord)
const fileAttente = computed(() =>
  demandes.value
    .filter((x) => x.etape === props.etape && x.id !== d.value?.id)
    .sort((a, b) => a.date.localeCompare(b.date)),
)

// Message après une décision (la demande quitte alors cette étape)
const decision = ref('')
watch(
  () => [props.etape, route.query.id],
  () => {
    decision.value = ''
    idDecide.value = null
  },
)

// À remplacer par l'appel à l'API (changement de idStatus)
// La date de décision de l'étape est gardée pour le suivi de la fiche
function noterDate() {
  idDecide.value = d.value.id
  d.value.historique = { ...d.value.historique, [props.etape]: aujourdhui() }
}

function valider() {
  noterDate()
  const suivante = etapeSuivante(props.etape)
  d.value.etape = suivante ? suivante.code : 'approuvee'
  decision.value = suivante
    ? `Demande n° ${d.value.id} validée : elle passe à l'étape ${suivante.label}.`
    : `Demande n° ${d.value.id} validée : elle est approuvée.`
}

function refuser() {
  noterDate()
  d.value.etape = 'refusee'
  d.value.refuseePar = labelEtape(props.etape)
  decision.value = `Demande n° ${d.value.id} refusée.`
}

// Passe à la demande suivante de la file
function suivante(id) {
  router.push(`/validation/${props.etape}?id=${id}`)
}
</script>

<template>
  <div v-if="!d" class="fiche">
    <Progression :etape="etape" />
    <p class="help">
      Aucune demande en attente à l'étape {{ labelEtape(etape) }}.
      <RouterLink v-if="utilisateur?.voitToutesLesDemandes" to="/toutes-les-demandes">Voir toutes les demandes</RouterLink>
    </p>
  </div>

  <FicheDemande v-else :demande="d">
    <template #cote>
      <section class="fiche-bloc decision">
        <h2>Décision {{ labelEtape(etape) }}</h2>

        <template v-if="d.etape === etape">
          <p class="help">Validez pour transmettre la demande, ou refusez-la pour l'arrêter.</p>
          <div class="actions">
            <button type="button" class="btn-success" @click="valider">Valider</button>
            <button type="button" class="btn-danger" @click="refuser">Refuser</button>
          </div>
        </template>
        <p v-else-if="decision" class="success">{{ decision }}</p>
        <p v-else-if="d.etape === 'approuvee'" class="success">Demande approuvée.</p>
        <p v-else-if="d.etape === 'refusee'" class="error">Demande refusée par {{ d.refuseePar }}.</p>
        <p v-else class="help">Cette demande est à l'étape {{ labelEtape(d.etape) }}, pas {{ labelEtape(etape) }}.</p>

        <RouterLink :to="`/demandes/${d.id}`">Voir la fiche seule</RouterLink>
      </section>

      <section class="fiche-bloc">
        <h2>En attente {{ labelEtape(etape) }} <span class="count">{{ fileAttente.length }}</span></h2>
        <ul v-if="fileAttente.length" class="file-attente">
          <li v-for="x in fileAttente" :key="x.id">
            <button type="button" @click="suivante(x.id)">
              <strong>N° {{ x.id }}</strong> {{ labelDepartement(x) }}
              <span class="help">{{ x.date }} · {{ totalPostes(x) }} poste(s)</span>
            </button>
          </li>
        </ul>
        <p v-else class="help">Aucune autre demande à traiter.</p>
      </section>
    </template>

    <footer class="fiche-actions">
      <RouterLink v-if="utilisateur?.voitToutesLesDemandes" to="/toutes-les-demandes">← Toutes les demandes</RouterLink>
    </footer>
  </FicheDemande>
</template>
