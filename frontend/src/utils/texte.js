// Comparaison de texte pour les recherches : insensible à la casse et aux accents
// (« e » trouve « é », « è », « ê », « ë » ; « c » trouve « ç » ; « oe » ne trouve pas « œ »).
export const normaliser = (texte) =>
  String(texte ?? '')
    .normalize('NFD') // « é » → « e » + accent combinant
    .replace(/[̀-ͯ]/g, '') // on retire les accents combinants
    .toLowerCase()

// Vrai si `texte` contient `recherche` (recherche vide = tout correspond)
export const contient = (texte, recherche) =>
  normaliser(texte).includes(normaliser(recherche).trim())
