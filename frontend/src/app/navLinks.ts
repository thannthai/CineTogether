import { browseHref } from '../features/catalog'
import type { NavKey } from './activeNav'

// The browse links of the header and the footer. TV Shows stays hidden until the backend can list it (see progress.md).
export const NAV_LINKS: { key: NavKey; label: string; to: string }[] = [
  { key: 'new', label: 'Phim mới', to: browseHref() },
  { key: 'movie', label: 'Phim lẻ', to: browseHref({ type: 'MOVIE' }) },
  { key: 'series', label: 'Phim bộ', to: browseHref({ type: 'SERIES' }) },
  { key: 'cinema', label: 'Chiếu rạp', to: browseHref({ list: 'CINEMA' }) },
  { key: 'anime', label: 'Anime', to: browseHref({ list: 'ANIME' }) },
]
