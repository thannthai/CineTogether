export const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2]

export type PlayerPrefs = { volume: number; muted: boolean; rate: number }
export const DEFAULT_PREFS: PlayerPrefs = { volume: 1, muted: false, rate: 1 }

const KEY = 'cinetogether-player'

// Reads what was saved, fixing every field on its own so one bad value never breaks the player.
export function parsePrefs(raw: string | null): PlayerPrefs {
  let data: unknown
  try {
    data = raw ? JSON.parse(raw) : null
  } catch {
    return DEFAULT_PREFS
  }
  if (typeof data !== 'object' || data === null) return DEFAULT_PREFS
  const { volume, muted, rate } = data as Record<string, unknown>
  return {
    volume: typeof volume === 'number' && volume >= 0 && volume <= 1 ? volume : DEFAULT_PREFS.volume,
    muted: typeof muted === 'boolean' ? muted : DEFAULT_PREFS.muted,
    rate: typeof rate === 'number' && SPEEDS.includes(rate) ? rate : DEFAULT_PREFS.rate,
  }
}

export function loadPrefs(): PlayerPrefs {
  try {
    return parsePrefs(localStorage.getItem(KEY))
  } catch {
    return DEFAULT_PREFS
  }
}

export function savePrefs(prefs: PlayerPrefs) {
  try {
    localStorage.setItem(KEY, JSON.stringify(prefs))
  } catch {
    // Storage unavailable: the choice lasts for this page only.
  }
}
