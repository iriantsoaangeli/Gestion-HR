<!-- TEMPORAIRE : démonstration des erreurs de validation d'une demande — à supprimer (avec sa route et son lien) -->
<script setup>
import AppLayout from '../../components/AppLayout.vue'
import Erreur from '../../components/Erreur.vue'
</script>

<template>
  <AppLayout title="Page temporaire : erreurs sur la validation">
    <div class="demo-cas">
      <h2>Demande introuvable</h2>
      <Erreur message="La demande n° 42 n'existe pas ou a été supprimée.">
        <RouterLink to="/mes-demandes">Voir mes demandes</RouterLink>
      </Erreur>

      <h2>Pas encore à cette étape</h2>
      <Erreur message="Cette demande attend encore la validation HR : la Direction ne peut pas la traiter." />

      <h2>Déjà traitée</h2>
      <Erreur message="Cette demande a déjà été validée par la Finance." />

      <h2>Droits insuffisants</h2>
      <Erreur titre="Accès refusé" message="Seul un membre de la Direction peut valider à cette étape." />

      <h2>Échec de l'enregistrement (fermable)</h2>
      <Erreur
        titre="Validation non enregistrée"
        message="Le serveur n'a pas répondu. La demande est restée à son étape, réessayez."
        fermable
      />
    </div>
  </AppLayout>
</template>
