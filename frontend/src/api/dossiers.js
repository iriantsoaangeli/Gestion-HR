import { offres } from './offres'

// Dépôt de dossier de candidature — DONNÉES STATIQUES : pas encore de backend.
// Les fonctions renvoient une promesse comme un vrai appel : pour brancher l'API plus tard,
// remplacer leur corps par appel('GET', '/api/...') (voir api/client.js) sans toucher aux composants.

// Personnes de la base (table personne). poste : poste occupé (table emploi) = employé, null = externe.
const PERSONNES = [
  // Employés (mêmes personnes que base/data.sql et base/demo.sql)
  {
    id: 1,
    nom: 'Rakotomalala',
    prenom: 'Hery',
    naissance: '1985-03-14',
    adresse: 'Lot II M 45 Analakely, Antananarivo',
    tel: '034 12 345 67',
    email: 'hery.rakotomalala@exemple.mg',
    poste: 'Manager informatique',
    departement: 'Informatique',
  },
  {
    id: 2,
    nom: 'Rasoanaivo',
    prenom: 'Lalao',
    naissance: '1979-07-02',
    adresse: 'Lot IVG 12 Ambohijatovo, Antananarivo',
    tel: '033 23 456 78',
    email: 'lalao.rasoanaivo@exemple.mg',
    poste: 'Responsable RH',
    departement: 'Ressources humaines',
  },
  {
    id: 3,
    nom: 'Andrianarivo',
    prenom: 'Fanja',
    naissance: '1968-11-23',
    adresse: 'Lot VA 8 Ivandry, Antananarivo',
    tel: '032 34 567 89',
    email: 'fanja.andrianarivo@exemple.mg',
    poste: 'Directeur général',
    departement: 'Direction',
  },
  {
    id: 4,
    nom: 'Razafindrakoto',
    prenom: 'Mamy',
    naissance: '1975-05-30',
    adresse: 'Lot III K 21 Ankorondrano, Antananarivo',
    tel: '034 45 678 90',
    email: 'mamy.razafindrakoto@exemple.mg',
    poste: 'Directeur financier',
    departement: 'Finance',
  },
  {
    id: 5,
    nom: 'Rabearivelo',
    prenom: 'Toky',
    naissance: '1990-01-09',
    adresse: 'Lot I B 3 Andravoahangy, Antananarivo',
    tel: '038 56 789 01',
    email: 'toky.rabearivelo@exemple.mg',
    poste: "Administrateur de l'application",
    departement: 'Informatique',
  },
  {
    id: 6,
    nom: 'Ravelojaona',
    prenom: 'Nirina',
    naissance: '1983-09-17',
    adresse: 'Lot II J 7 Isotry, Antananarivo',
    tel: '034 67 890 12',
    email: 'nirina.ravelojaona@exemple.mg',
    poste: 'Manager RH',
    departement: 'Ressources humaines',
  },
  {
    id: 7,
    nom: 'Randrianasolo',
    prenom: 'Fidy',
    naissance: '1987-04-05',
    adresse: 'Lot IVF 30 Ampefiloha, Antananarivo',
    tel: '033 78 901 23',
    email: 'fidy.randrianasolo@exemple.mg',
    poste: 'Manager finance',
    departement: 'Finance',
  },
  // Externes : déjà dans la base (anciennes candidatures), sans poste
  {
    id: 8,
    nom: 'Rakotoarisoa',
    prenom: 'Andry',
    naissance: '1996-02-11',
    adresse: 'Lot VB 41 Ambatobe, Antananarivo',
    tel: '034 11 222 33',
    email: 'andry.rakotoarisoa@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 9,
    nom: 'Ramanantsoa',
    prenom: 'Volana',
    naissance: '1993-08-24',
    adresse: 'Lot III A 9 Ankadifotsy, Antananarivo',
    tel: '033 22 333 44',
    email: 'volana.ramanantsoa@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 10,
    nom: 'Rasolofonirina',
    prenom: 'Tiana',
    naissance: '1998-12-03',
    adresse: 'Lot II F 17 Ambanidia, Antananarivo',
    tel: '032 33 444 55',
    email: 'tiana.rasolofonirina@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 11,
    nom: 'Andriamanana',
    prenom: 'Haja',
    naissance: '1989-06-15',
    adresse: 'Lot IVL 2 Ampasampito, Antananarivo',
    tel: '034 44 555 66',
    email: 'haja.andriamanana@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 12,
    nom: 'Rahelimalala',
    prenom: 'Soa',
    naissance: '1995-04-28',
    adresse: 'Lot I G 55 Behoririka, Antananarivo',
    tel: '038 55 666 77',
    email: 'soa.rahelimalala@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 13,
    nom: 'Randriamampionona',
    prenom: 'Éric',
    naissance: '1991-10-19',
    adresse: 'Lot VD 6 Ambohipo, Antananarivo',
    tel: '034 66 777 88',
    email: 'eric.randriamampionona@exemple.mg',
    poste: null,
    departement: null,
  },
  {
    id: 14,
    nom: 'Rakotondrabe',
    prenom: 'Mialy',
    naissance: '1997-01-07',
    adresse: 'Lot II Y 30 Mahamasina, Antananarivo',
    tel: '033 77 888 99',
    email: 'mialy.rakotondrabe@exemple.mg',
    poste: null,
    departement: null,
  },
]

export const personnes = () => Promise.resolve(structuredClone(PERSONNES))
export const offresOuvertes = () =>
  offres().then((liste) => liste.filter((o) => o.status === 'OUVERT'))

// Dossiers de candidature (table dossier : id, date, idOffre, idPersonne), plus le candidat et les
// pièces jointes (pas encore de table pour les fichiers). Les dossiers 1 à 8 ont un entretien
// (api/entretiens.js), les autres pas encore.
const DOSSIERS = [
  {
    id: 1,
    date: '2026-11-02T10:15:00',
    idOffre: 1,
    idPersonne: 8,
    fichiers: ['CV.pdf', 'Lettre.pdf'],
  },
  { id: 2, date: '2026-11-03T09:40:00', idOffre: 1, idPersonne: 9, fichiers: ['CV.pdf'] },
  {
    id: 3,
    date: '2026-11-04T14:05:00',
    idOffre: 2,
    idPersonne: 10,
    fichiers: ['CV.pdf', 'Portfolio.pdf'],
  },
  { id: 4, date: '2026-11-05T11:20:00', idOffre: 4, idPersonne: 11, fichiers: ['CV.pdf'] },
  {
    id: 5,
    date: '2026-11-06T16:30:00',
    idOffre: 5,
    idPersonne: 12,
    fichiers: ['CV.pdf', 'Lettre.pdf'],
  },
  {
    id: 6,
    date: '2026-10-28T08:50:00',
    idOffre: 3,
    idPersonne: 13,
    fichiers: ['CV.pdf', 'Diplôme.pdf'],
  },
  { id: 7, date: '2026-10-29T13:10:00', idOffre: 3, idPersonne: 14, fichiers: ['CV.pdf'] },
  {
    id: 8,
    date: '2026-10-30T10:00:00',
    idOffre: 2,
    idPersonne: 8,
    fichiers: ['CV.pdf', 'Lettre.pdf'],
  },
  { id: 9, date: '2026-11-08T09:00:00', idOffre: 1, idPersonne: 10, fichiers: ['CV.pdf'] },
  {
    id: 10,
    date: '2026-11-09T15:45:00',
    idOffre: 2,
    idPersonne: 12,
    fichiers: ['CV.pdf', 'Lettre.pdf'],
  },
  { id: 11, date: '2026-11-10T10:30:00', idOffre: 4, idPersonne: 5, fichiers: ['CV.pdf'] },
  {
    id: 12,
    date: '2026-11-11T11:00:00',
    idOffre: 6,
    idPersonne: 9,
    fichiers: ['CV.pdf', 'Lettre.pdf'],
  },
]

// Dossier tel que l'API le renverra : avec le candidat (nom complet, employé ou externe)
const versReponse = (d) => {
  const p = PERSONNES.find((x) => x.id === d.idPersonne)
  return {
    ...d,
    candidat: `${p.prenom} ${p.nom}`,
    email: p.email,
    tel: p.tel,
    employe: Boolean(p.poste),
  }
}

export const dossiers = () => Promise.resolve(DOSSIERS.map(versReponse))
export const dossiersDeLOffre = (idOffre) =>
  dossiers().then((liste) => liste.filter((d) => d.idOffre === Number(idOffre)))
