<script setup>
import { labelDepartement, labelPostes } from '../data/demandes'

// lien : le n° ouvre la demande à son étape ; refus : ajoute la colonne « Refusée par »
defineProps({
  titre: String,
  demandes: Array,
  lien: Boolean,
  refus: Boolean,
})
</script>

<template>
  <section class="colonne">
    <h2>{{ titre }} <span class="count">{{ demandes.length }}</span></h2>
    <p v-if="!demandes.length" class="vide">Pas de demande</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>N°</th><th>Date</th><th>Département</th><th>Postes</th><th>Eff.</th>
            <th v-if="refus">Refusée par</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in demandes" :key="d.id" :title="d.motif">
            <td>
              <RouterLink v-if="lien" :to="`/demande/${d.etape}?id=${d.id}`">{{ d.id }}</RouterLink>
              <template v-else>{{ d.id }}</template>
            </td>
            <td>{{ d.date }}</td>
            <td>{{ labelDepartement(d) }}</td>
            <td>{{ labelPostes(d) }}</td>
            <td>{{ d.effectif }}</td>
            <td v-if="refus" class="error">{{ d.refuseePar }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
