<!-- TEMPORAIRE : démonstration des erreurs du login — à supprimer (avec sa route et son lien) -->
<script setup>
import Erreur from '../../components/Erreur.vue'
</script>

<template>
  <div class="center demo-erreurs">
    <p class="demo-bandeau">Page temporaire : erreurs possibles sur le login</p>

    <form class="card form" @submit.prevent>
      <h1>Gestion RH</h1>
      <label for="demo-identifiant">Identifiant</label>
      <input id="demo-identifiant" value="rakoto" autocomplete="off" />
      <label for="demo-mdp">Mot de passe</label>
      <input id="demo-mdp" type="password" value="xxxxxx" autocomplete="off" />
      <Erreur message="Identifiant ou mot de passe incorrect." />
      <button type="submit">Se connecter</button>
    </form>

    <div class="demo-cas">
      <h2>Champs vides</h2>
      <Erreur message="Veuillez remplir tous les champs." />

      <h2>Compte désactivé</h2>
      <Erreur titre="Compte désactivé" message="Contactez le service RH pour réactiver votre accès." />

      <h2>Serveur injoignable (fermable)</h2>
      <Erreur
        titre="Connexion impossible"
        message="Le serveur ne répond pas. Réessayez dans quelques instants."
        fermable
      />
    </div>
  </div>
</template>
