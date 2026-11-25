export type MovieTypeFilter = 'MOVIE' | 'SERIES'

// Field names match the GET /movies query parameters in docs/design/api.md.
export type BrowseFilters = {
  type?: MovieTypeFilter
  genre?: string
  country?: string
  q?: string
}

export function browseHref(filters: BrowseFilters = {}): string {
  const params = new URLSearchParams()
  if (filters.type) params.set('type', filters.type)
  if (filters.genre) params.set('genre', filters.genre)
  if (filters.country) params.set('country', filters.country)
  const q = filters.q?.trim()
  if (q) params.set('q', q)
  const query = params.toString()
  return query ? `/movies?${query}` : '/movies'
}

export function parseBrowseFilters(params: URLSearchParams): BrowseFilters {
  const type = params.get('type')
  return {
    type: type === 'MOVIE' || type === 'SERIES' ? type : undefined,
    genre: params.get('genre') || undefined,
    country: params.get('country') || undefined,
    q: params.get('q')?.trim() || undefined,
  }
}
