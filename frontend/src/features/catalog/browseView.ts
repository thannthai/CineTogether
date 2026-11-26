import type { BrowseFilters } from './browseFilters'
import { COUNTRIES, GENRES, type FilterOption } from './filters'

const nameOf = (list: FilterOption[], slug: string) => list.find((o) => o.slug === slug)?.name ?? slug

// The heading of the browse page for the current filters.
export function browseTitle({ type, genre, country, q }: BrowseFilters): string {
  if (q) return `Kết quả cho “${q}”`
  const parts = [
    type === 'MOVIE' ? 'Phim lẻ' : type === 'SERIES' ? 'Phim bộ' : null,
    genre ? nameOf(GENRES, genre) : null,
    country ? nameOf(COUNTRIES, country) : null,
  ].filter((part): part is string => part !== null)
  return parts.length > 0 ? parts.join(' · ') : 'Phim mới cập nhật'
}

export const activeFilterCount = ({ type, genre, country }: BrowseFilters): number =>
  [type, genre, country].filter(Boolean).length

export type PageWindow = { items: (number | '…')[]; prev: number | null; next: number | null }

// The API only says whether a next page exists, so the window ends at the next page and never
// shows a last page number: up to two earlier pages, the current one, and the next if it exists.
export function pageWindow(current: number, hasNext: boolean): PageWindow {
  const start = Math.max(1, current - 2)
  const end = hasNext ? current + 1 : current
  const items: (number | '…')[] = []
  if (start > 1) {
    items.push(1)
    if (start > 2) items.push('…')
  }
  for (let page = start; page <= end; page++) items.push(page)
  return { items, prev: current > 1 ? current - 1 : null, next: hasNext ? current + 1 : null }
}
