// Appels à l'API d'authentification du backend (Spring Security, session par cookie).
// En dev, Vite redirige /api vers http://localhost:8080 (vite.config.js) : même origine, pas de CORS.
// CSRF : le backend pose le cookie XSRF-TOKEN, on le renvoie dans l'en-tête X-XSRF-TOKEN sur les POST.

// Erreur renvoyée par l'API : { statut, code, message }
export class ErreurApi extends Error {
  constructor(statut, code, message) {
    super(message)
    this.statut = statut
    this.code = code
  }
}

const lireCookie = (nom) =>
  document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${nom}=`))
    ?.slice(nom.length + 1)

// Pose le cookie XSRF-TOKEN s'il n'existe pas encore (avant le login, ou après une déconnexion)
async function jetonCsrf() {
  if (!lireCookie('XSRF-TOKEN')) await fetch('/api/auth/csrf', { credentials: 'include' })
  return decodeURIComponent(lireCookie('XSRF-TOKEN') ?? '')
}

// Appel générique : renvoie le JSON (ou null si 204), lève ErreurApi sinon
export async function appel(methode, url, corps) {
  const entetes = { Accept: 'application/json' }
  if (corps !== undefined) entetes['Content-Type'] = 'application/json'
  if (methode !== 'GET') entetes['X-XSRF-TOKEN'] = await jetonCsrf()

  let reponse
  try {
    reponse = await fetch(url, {
      method: methode,
      credentials: 'include',
      headers: entetes,
      body: corps === undefined ? undefined : JSON.stringify(corps),
    })
  } catch {
    throw new ErreurApi(0, 'SERVEUR_INJOIGNABLE', 'Le serveur ne répond pas. Réessayez dans quelques instants.')
  }

  if (reponse.status === 204) return null
  const json = await reponse.json().catch(() => null)
  if (!reponse.ok) {
    throw new ErreurApi(reponse.status, json?.code ?? 'ERREUR', json?.message ?? `Erreur ${reponse.status}.`)
  }
  return json
}

// POST /api/auth/login → { id, login, role, roleLibelle }
export const login = (login, mdp) => appel('POST', '/api/auth/login', { login, mdp })

// POST /api/auth/logout → ferme la session côté serveur
export const logout = () => appel('POST', '/api/auth/logout')

// GET /api/auth/moi → utilisateur connecté, ErreurApi 401 sinon
export const moi = () => appel('GET', '/api/auth/moi')
