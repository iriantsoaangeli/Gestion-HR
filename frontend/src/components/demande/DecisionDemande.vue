<script setup>
import { computed, ref, watch } from 'vue'
import Erreur from '../ui/Erreur.vue'
import * as api from '../../api/demandes'
import { useChargement } from '../../utils/chargement'
import { totalPostes } from '../../utils/demande'

// Panneau de décision, affiché dans la fiche quand c'est à moi de décider (demande.aMoiDeDecider) :
// Valider / Refuser (le backend vérifie que c'est bien mon étape), puis la file des autres demandes
// qui attendent ma décision. Émet « decidee » avec la demande mise à jour.
const props = defineProps({ demande: { type: Object, required: true } })
const emit = defineEmits(['decidee'])

const message = ref('')
const erreur = ref('')
const envoi = ref(false)

const { donnees: file, recharger } = useChargement(api.aTraiter, [])
const autres = computed(() => file.value.filter((x) => x.id !== props.demande.id))

// Nouvelle demande affichée : on efface le message de la précédente
watch(
  () => props.demande.id,
  () => {
    message.value = ''
    erreur.value = ''
  },
)

async function decider(action) {
  envoi.value = true
  erreur.value = ''
  try {
    const d = await (action === 'valider' ? api.valider : api.refuser)(props.demande.id)
    message.value = action === 'valider'
      ? `Demande n° ${d.id} validée : ${d.statutLibelle.toLowerCase()}.`
      : `Demande n° ${d.id} refusée.`
    emit('decidee', d)
    recharger()
  } catch (e) {
    erreur.value = e.message
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <section class="fiche-bloc decision">
    <h2>Ma décision</h2>

    <template v-if="demande.aMoiDeDecider">
      <p class="help">Validez pour transmettre la demande à l'étape suivante, ou refusez-la pour l'arrêter.</p>
      <div class="actions">
        <button type="button" class="btn-success" :disabled="envoi" @click="decider('valider')">Valider</button>
        <button type="button" class="btn-danger" :disabled="envoi" @click="decider('refuser')">Refuser</button>
      </div>
    </template>
    <p v-else-if="message" class="success">{{ message }}</p>

    <Erreur :message="erreur" />
  </section>

  <section class="fiche-bloc">
    <h2>Attendent ma décision <span class="count">{{ autres.length }}</span></h2>
    <ul v-if="autres.length" class="file-attente">
      <li v-for="x in autres" :key="x.id">
        <RouterLink :to="`/demandes/${x.id}`">
          <strong>N° {{ x.id }}</strong> {{ x.departement }}
          <span class="help">{{ x.date }} · {{ totalPostes(x) }} poste(s)</span>
        </RouterLink>
      </li>
    </ul>
    <p v-else class="help">Aucune autre demande à traiter.</p>
  </section>
</template>
