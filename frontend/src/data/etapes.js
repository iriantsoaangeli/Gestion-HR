// Workflow d'une demande : Manager -> HR -> Direction -> Finance (cf. regle-metier/bloc1.md)
export const ETAPES = [
  { code: 'manager', label: 'Manager' },
  { code: 'hr', label: 'HR' },
  { code: 'direction', label: 'Direction' },
  { code: 'finance', label: 'Finance' },
]

export const etapeSuivante = (code) => ETAPES[ETAPES.findIndex((e) => e.code === code) + 1]
export const labelEtape = (code) => ETAPES.find((e) => e.code === code)?.label
