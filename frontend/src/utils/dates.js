// Dates au format AAAA-MM-JJ (celui de l'API), en heure locale

export const iso = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// Aujourd'hui décalé de quelques jours / mois
export function dansLe({ jours = 0, mois = 0 } = {}) {
  const d = new Date()
  d.setMonth(d.getMonth() + mois)
  d.setDate(d.getDate() + jours)
  return iso(d)
}

// Partie AAAA-MM-JJ d'une date ou d'une date-heure de l'API (« 2026-11-11T10:23:45 » → « 2026-11-11 »)
export const jour = (texte) => (texte ? String(texte).slice(0, 10) : texte)

// « 11 novembre 2026 »
export const dateLongue = (texte) =>
  new Date(`${jour(texte)}T00:00:00`).toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })

// Jours restants avant une date (négatif si dépassée), null sans date
export function joursAvant(texte) {
  if (!texte) return null
  const aujourdhui = new Date()
  aujourdhui.setHours(0, 0, 0, 0)
  return Math.round((new Date(`${jour(texte)}T00:00:00`) - aujourdhui) / 86400000)
}

// « 18/11/2026 » (null sans date) ; accepte une date ou une date-heure de l'API
export const dateCourte = (texte) =>
  texte ? new Date(`${jour(texte)}T00:00:00`).toLocaleDateString('fr-FR') : null

// « 18/11/2026 09:00 » (null sans date)
export const dateHeure = (texte) =>
  texte ? new Date(texte).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : null
