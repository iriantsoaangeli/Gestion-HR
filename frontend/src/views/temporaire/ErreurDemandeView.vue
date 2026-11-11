<!-- TEMPORAIRE : démonstration des erreurs du formulaire de demande — à supprimer (avec sa route et son lien) -->
<script setup>
import AppLayout from '../../components/AppLayout.vue'
import Erreur from '../../components/Erreur.vue'
</script>

<template>
  <AppLayout title="Page temporaire : erreurs sur la demande">
    <div class="demo-cas">
      <h2>Aucun poste</h2>
      <Erreur message="Ajoutez ou remplacez au moins un poste." />

      <h2>Délai trop court</h2>
      <Erreur message="Le délai doit être d'au moins une semaine." />

      <h2>Plusieurs erreurs à la fois</h2>
      <Erreur
        titre="La demande n'a pas pu être envoyée :"
        :message="[
          'Choisissez un département.',
          'Ajoutez ou remplacez au moins un poste.',
          'Le motif est obligatoire.',
        ]"
      />

      <h2>Poste déjà demandé</h2>
      <Erreur message="« Comptable » fait déjà l'objet d'une demande en cours (n° 3)." />

      <h2>Échec de l'envoi (fermable)</h2>
      <Erreur
        titre="Envoi impossible"
        message="Le serveur n'a pas enregistré la demande. Vos saisies sont conservées, réessayez."
        fermable
      />
    </div>
  </AppLayout>
</template>
