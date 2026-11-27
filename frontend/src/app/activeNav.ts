import { parseBrowseFilters } from '../features/catalog'

export type NavKey = 'new' | 'movie' | 'series' | 'cinema' | 'anime' | 'genre' | 'country' | 'together'

// Which header item to highlight for the current URL; null when none fits.
export function activeNav(pathname: string, params: URLSearchParams): NavKey | null {
  if (pathname === '/together') return 'together'
  if (pathname !== '/movies') return null

  const { type, list, genre, country, q } = parseBrowseFilters(params)
  if (q) return null
  if (list === 'CINEMA') return 'cinema'
  if (list === 'ANIME') return 'anime'
  if (type === 'MOVIE') return 'movie'
  if (type === 'SERIES') return 'series'
  if (genre && country) return null
  if (genre) return 'genre'
  if (country) return 'country'
  return 'new'
}
