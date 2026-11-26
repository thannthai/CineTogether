import type { EpisodeProgress, MovieType } from './types'

// "Tập 8/24" while a series airs, "Hoàn tất 36/36" when it is finished, "Full" for a single movie.
export function progressLabel(progress: EpisodeProgress | null | undefined, type: MovieType): string | null {
  if (!progress) return null
  if (type === 'MOVIE') return progress.completed ? 'Full' : null

  const { current, total, completed } = progress
  if (completed) {
    const known = total ?? current
    return known ? `Hoàn tất ${current ?? known}/${total ?? known}` : 'Hoàn tất'
  }
  if (current == null) return null
  return total ? `Tập ${current}/${total}` : `Tập ${current}`
}

const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/
const YOUTUBE_HOSTS = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtube-nocookie.com', 'www.youtube-nocookie.com'])

// The id of the video in a YouTube link, or null. Only a well-formed id is returned, so it is safe to put in an
// embed address.
export function youtubeId(link: string | null | undefined): string | null {
  if (!link) return null
  let url: URL
  try {
    url = new URL(link)
  } catch {
    return null
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') return null

  let id: string | null = null
  if (url.hostname === 'youtu.be') {
    id = url.pathname.slice(1)
  } else if (YOUTUBE_HOSTS.has(url.hostname)) {
    const [, kind, rest] = url.pathname.split('/')
    id = kind === 'watch' ? url.searchParams.get('v') : kind === 'embed' || kind === 'shorts' ? rest : null
  }
  return id && VIDEO_ID.test(id) ? id : null
}

// The original (usually English) title is shown next to the Vietnamese one only when there is one that differs.
export const differentTitle = ({ title, originalTitle }: { title: string; originalTitle?: string | null }): boolean =>
  !!originalTitle?.trim() && originalTitle.trim().toLowerCase() !== title.trim().toLowerCase()
