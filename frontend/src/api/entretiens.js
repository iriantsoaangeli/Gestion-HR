import { dossiers } from './dossiers'
import { offres } from './offres'
import { utilisateur } from '../auth/session'
import { iso } from '../utils/dates'

// Entretiens de recrutement — DONNÉES STATIQUES : pas encore de backend.
// Les fonctions renvoient une promesse comme un vrai appel : pour brancher l'API plus tard,
// remplacer leur corps par appel('GET', '/api/...') (voir api/client.js) sans toucher aux composants.
// Les ajouts (preparer, evaluer) restent en mémoire jusqu'au rechargement de la page.

// Ceux qui peuvent faire un entretien : les postes des comptes de l'application (base/data.sql),
// sauf FINANCE et ADMIN. login = compte qui tient le poste (pour savoir qui est connecté).
const INTERVIEWERS = [
  {
    idPoste: 6,
    poste: 'Manager informatique',
    departement: 'Informatique',
    detenteur: 'Hery Rakotomalala',
    login: 'manager',
  },
  {
    idPoste: 7,
    poste: 'Responsable RH',
    departement: 'Ressources humaines',
    detenteur: 'Lalao Rasoanaivo',
    login: 'hr',
  },
  {
    idPoste: 8,
    poste: 'Directeur général',
    departement: 'Direction',
    detenteur: 'Fanja Andrianarivo',
    login: 'direction',
  },
]

// Table entretien (id, date, idDossier, idStatusOffre) + ceux qui le font (interviewers : idPoste)
// + une évaluation par interviewer (table evaluation : note /100, remarques ; idPoste = qui l'a écrite,
// pas encore dans le schéma).
const ENTRETIENS = [
  { id: 1, date: '2026-11-18T09:00:00', idDossier: 1, interviewers: [7], evaluations: [] },
  { id: 2, date: '2026-11-18T14:30:00', idDossier: 2, interviewers: [7], evaluations: [] },
  {
    id: 3,
    date: '2026-11-20T10:00:00',
    idDossier: 3,
    interviewers: [6, 7],
    evaluations: [],
  },
  {
    id: 4,
    date: '2026-11-10T08:30:00',
    idDossier: 4,
    interviewers: [6, 7],
    evaluations: [
      {
        id: 1,
        idPoste: 7,
        date: '2026-11-10T11:00:00',
        note: 70,
        remarques: 'Bon contact, connaît bien Linux.',
      },
    ],
  },
  { id: 5, date: '2026-11-25T15:00:00', idDossier: 5, interviewers: [7, 8], evaluations: [] },
  {
    id: 6,
    date: '2026-11-05T09:30:00',
    idDossier: 6,
    interviewers: [7],
    evaluations: [
      {
        id: 2,
        idPoste: 7,
        date: '2026-11-05T12:00:00',
        note: 78,
        remarques: 'Bonne maîtrise de la paie, à l’aise avec les déclarations sociales.',
      },
    ],
  },
  {
    id: 7,
    date: '2026-11-06T11:00:00',
    idDossier: 7,
    interviewers: [7],
    evaluations: [
      {
        id: 3,
        idPoste: 7,
        date: '2026-11-06T14:00:00',
        note: 64,
        remarques: 'Motivée, mais peu d’expérience sur un logiciel de paie.',
      },
    ],
  },
  {
    id: 8,
    date: '2026-11-09T14:00:00',
    idDossier: 8,
    interviewers: [6, 7],
    evaluations: [
      {
        id: 4,
        idPoste: 6,
        date: '2026-11-09T16:00:00',
        note: 85,
        remarques: 'Très bon test technique (Vue.js et Spring Boot).',
      },
      {
        id: 5,
        idPoste: 7,
        date: '2026-11-09T16:30:00',
        note: 80,
        remarques: 'Motivé, disponible tout de suite.',
      },
    ],
  },
]

// Date-heure locale au format de l'API (« 2026-11-12T16:05:00 ») ; toISOString donnerait l'heure UTC
const maintenant = () => {
  const d = new Date()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${iso(d)}T${hh}:${mm}:00`
}

const interviewer = (idPoste) => INTERVIEWERS.find((i) => i.idPoste === idPoste)

// Notes séparées : un interviewer qui n'a pas encore rendu son évaluation ne voit ni la note ni les
// remarques des autres (il voit seulement qui a évalué). Le backend fera ce filtre ; ici, données statiques.
function masquerNotes(r) {
  const moi = r.interviewers.find((i) => i.login === utilisateur.value?.login)
  const cachees = Boolean(moi) && !r.evaluations.some((v) => v.idPoste === moi.idPoste)
  if (!cachees) return { ...r, notesCachees: false }
  return {
    ...r,
    notesCachees: true,
    note: null,
    evaluations: r.evaluations.map((v) => ({ ...v, note: null, remarques: null })),
  }
}

// Entretien tel que l'API le renverra : offre et candidat lus par le dossier, interviewers et
// évaluations avec leur poste, statut calculé (fini = tous les interviewers ont évalué),
// note = moyenne des évaluations (null s'il n'y en a pas), notes des autres cachées (masquerNotes).
async function versReponse(e) {
  const dossier = (await dossiers()).find((d) => d.id === e.idDossier)
  const offre = (await offres()).find((o) => o.id === dossier.idOffre)
  const evaluations = e.evaluations.map((v) => ({ ...v, ...interviewer(v.idPoste) }))
  const fini = e.interviewers.every((id) => evaluations.some((v) => v.idPoste === id))
  return masquerNotes({
    id: e.id,
    date: e.date,
    idDossier: e.idDossier,
    idOffre: offre.id,
    poste: offre.poste,
    departement: offre.departement,
    idPersonne: dossier.idPersonne,
    candidat: dossier.candidat,
    interviewers: e.interviewers.map(interviewer),
    evaluations,
    statut: fini ? 'FINI' : 'EN_ATTENTE',
    statutLibelle: fini ? 'Fini' : 'En attente',
    note: evaluations.length
      ? Math.round(evaluations.reduce((s, v) => s + v.note, 0) / evaluations.length)
      : null,
  })
}

const tous = () => Promise.all(ENTRETIENS.map(versReponse))

export const enAttente = () => tous().then((l) => l.filter((e) => e.statut === 'EN_ATTENTE'))
export const finis = () => tous().then((l) => l.filter((e) => e.statut === 'FINI'))
export const entretiensDeLOffre = (idOffre) =>
  tous().then((l) => l.filter((e) => e.idOffre === Number(idOffre)))

// Évaluations de l'utilisateur connecté (le backend le lira dans la session).
// DONNÉES STATIQUES : son poste d'interviewer est retrouvé par son login.
// aEvaluer : entretiens dont il est interviewer et qu'il n'a pas encore évalués ;
// evaluesParMoi : ceux qu'il a déjà évalués, avec sa note (maNote).
const monPoste = (e) => e.interviewers.find((i) => i.login === utilisateur.value?.login)?.idPoste
const monEvaluation = (e) => e.evaluations.find((v) => v.idPoste === monPoste(e))

export const aEvaluer = () => tous().then((l) => l.filter((e) => monPoste(e) && !monEvaluation(e)))
export const evaluesParMoi = () =>
  tous().then((l) =>
    l
      .filter(monEvaluation)
      .map((e) => ({ ...e, maNote: monEvaluation(e).note, idEvaluation: monEvaluation(e).id })),
  )

// Une évaluation (table evaluation) avec son évaluateur et l'entretien évalué
export async function evaluation(id) {
  const e = ENTRETIENS.find((x) => x.evaluations.some((v) => v.id === Number(id)))
  if (!e) throw new Error('Évaluation introuvable')
  const entretien = await versReponse(e)
  if (entretien.notesCachees)
    throw new Error("Rendez d'abord votre évaluation pour voir celles des autres interviewers.")
  return { ...entretien.evaluations.find((v) => v.id === Number(id)), entretien }
}

export async function entretien(id) {
  const e = ENTRETIENS.find((x) => x.id === Number(id))
  if (!e) throw new Error('Entretien introuvable')
  return versReponse(e)
}

// Postes qui peuvent faire un entretien (formulaire de préparation)
export const interviewersPossibles = () => Promise.resolve(structuredClone(INTERVIEWERS))

// Préparer un entretien (HR) : { idDossier, date: 'AAAA-MM-JJTHH:MM:00', interviewers: [idPoste] }
export async function preparer({ idDossier, date, interviewers }) {
  const e = {
    id: Math.max(...ENTRETIENS.map((x) => x.id)) + 1,
    date,
    idDossier,
    interviewers,
    evaluations: [],
  }
  ENTRETIENS.push(e)
  return versReponse(e)
}

// Évaluer un entretien : { idPoste, note, remarques } ; une seule évaluation par interviewer
export async function evaluer(idEntretien, { idPoste, note, remarques }) {
  const e = ENTRETIENS.find((x) => x.id === Number(idEntretien))
  if (!e) throw new Error('Entretien introuvable')
  if (!e.interviewers.includes(idPoste))
    throw new Error("Vous n'êtes pas interviewer de cet entretien.")
  if (e.evaluations.some((v) => v.idPoste === idPoste))
    throw new Error('Vous avez déjà évalué cet entretien.')
  const id = Math.max(0, ...ENTRETIENS.flatMap((x) => x.evaluations.map((v) => v.id))) + 1
  e.evaluations.push({ id, idPoste, date: maintenant(), note, remarques })
  return { ...(await versReponse(e)), idEvaluation: id } // idEvaluation : pour ouvrir sa fiche
}
