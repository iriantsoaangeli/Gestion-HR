<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { demandes, trouverDemande, labelDepartement, labelAjouts, labelRemplacements } from '../data/demandes'
import { etapeSuivante, labelEtape } from '../data/etapes'

const props = defineProps({ etape: { type: String, required: true } })
const route = useRoute()
const router = useRouter()

// Demande choisie (?id=), sinon la première en attente à cette étape
const d = computed(() =>
  route.query.id
    ? trouverDemande(Number(route.query.id))
    : demandes.value.find((x) => x.etape === props.etape),
)

// À remplacer par l'appel à l'API (changement de idStatus)
function valider() {
  const suivante = etapeSuivante(props.etape)
  d.value.etape = suivante ? suivante.code : 'approuvee'
  if (suivante) router.push(`/demande/${suivante.code}?id=${d.value.id}`)
}

function refuser() {
  d.value.etape = 'refusee'
  d.value.refuseePar = labelEtape(props.etape)
}
</script>

<template>
  <p v-if="!d" class="help">
    Aucune demande en attente ici. <RouterLink to="/mes-demandes">Voir mes demandes</RouterLink>
  </p>

  <div v-else class="resume">
    <dl>
      <dt>N°</dt><dd>{{ d.id }}</dd>
      <dt>Département</dt><dd>{{ labelDepartement(d) }}</dd>
      <dt>Postes à ajouter</dt><dd>{{ labelAjouts(d) || '—' }}</dd>
      <dt>Postes à remplir</dt><dd>{{ labelRemplacements(d) || '—' }}</dd>
      <dt>Date</dt><dd>{{ d.date }}</dd>
      <dt>Délai</dt><dd>{{ d.delai || 'Aucun' }}</dd>
      <dt>Motif</dt><dd>{{ d.motif }}</dd>
    </dl>

    <div v-if="d.etape === etape" class="actions">
      <button type="button" class="btn-success" @click="valider">Valider</button>
      <button type="button" class="btn-danger" @click="refuser">Refuser</button>
    </div>
    <p v-else-if="d.etape === 'approuvee'" class="success">Demande approuvée.</p>
    <p v-else-if="d.etape === 'refusee'" class="error">Demande refusée par {{ d.refuseePar }}.</p>
    <p v-else class="help">La demande est en attente à l'étape {{ labelEtape(d.etape) }}.</p>
  </div>
</template>
