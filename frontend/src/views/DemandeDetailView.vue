<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'
import FicheDemande from '../components/demande/FicheDemande.vue'
import Erreur from '../components/ui/Erreur.vue'
import { trouverDemande } from '../data/demandes'

// Fiche d'une demande : /demandes/:id
const props = defineProps({ id: { type: String, required: true } })

const d = computed(() => trouverDemande(Number(props.id)))

// Retour à la page d'où l'on vient (Mes demandes, Toutes les demandes…), sinon Mes demandes
const router = useRouter()
const retour = () => (window.history.state?.back ? router.back() : router.push('/mes-demandes'))
</script>

<template>
  <AppLayout>
    <template v-if="!d">
      <h1>Fiche demande</h1>
      <Erreur :message="`La demande n° ${id} n'existe pas ou a été supprimée.`">
        <RouterLink to="/mes-demandes">Voir mes demandes</RouterLink>
      </Erreur>
    </template>

    <FicheDemande v-else :demande="d">
      <footer class="fiche-actions">
        <a href="#" @click.prevent="retour">← Retour</a>
      </footer>
    </FicheDemande>
  </AppLayout>
</template>
