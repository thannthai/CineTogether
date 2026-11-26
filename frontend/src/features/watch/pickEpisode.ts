import type { Episode } from '../catalog'

// The episode asked for in the URL, or the first one when there is none or it is unknown.
export function pickEpisode(episodes: Episode[], wanted: string | null): { episode: Episode; index: number } | null {
  if (episodes.length === 0) return null
  const index = Math.max(
    0,
    episodes.findIndex((episode) => episode.episodeId === wanted),
  )
  return { episode: episodes[index], index }
}

// The episodes around the one at `index`; null where there is none (first and last episode).
export function neighbours(episodes: Episode[], index: number): { previous: Episode | null; next: Episode | null } {
  return { previous: episodes[index - 1] ?? null, next: episodes[index + 1] ?? null }
}
