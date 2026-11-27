export type MovieTypeFilter = 'MOVIE' | 'SERIES'

// A curated provider list. When it is set the API ignores `type`.
export type MovieListFilter = 'CINEMA' | 'ANIME'

// Field names match the GET /movies query parameters in docs/design/api.md.
export type BrowseFilters = {
  type?: MovieTypeFilter
  list?: MovieListFilter
  genre?: string
  country?: string
  year?: number
  q?: string
}

// `page` is the human page number, starting at 1 (the API itself starts at 0).
export function browseHref(filters: BrowseFilters = {}, page = 1): string {
  const params = new URLSearchParams()
  if (filters.type) params.set('type', filters.type)
  if (filters.list) params.set('list', filters.list)
  if (filters.genre) params.set('genre', filters.genre)
  if (filters.country) params.set('country', filters.country)
  if (filters.year) params.set('year', String(filters.year))
  const q = filters.q?.trim()
  if (q) params.set('q', q)
  if (page > 1) params.set('page', String(page))
  const query = params.toString()
  return query ? `/movies?${query}` : '/movies'
}

export function parseBrowseFilters(params: URLSearchParams): BrowseFilters {
  const type = params.get('type')
  const list = params.get('list')
  // The same range as the API (docs/design/api.md); anything else is ignored.
  const year = Number(params.get('year'))
  return {
    type: type === 'MOVIE' || type === 'SERIES' ? type : undefined,
    list: list === 'CINEMA' || list === 'ANIME' ? list : undefined,
    genre: params.get('genre') || undefined,
    country: params.get('country') || undefined,
    year: Number.isInteger(year) && year >= 1900 && year <= 2100 ? year : undefined,
    q: params.get('q')?.trim() || undefined,
  }
}

export function parsePage(params: URLSearchParams): number {
  const page = Number(params.get('page'))
  return Number.isInteger(page) && page >= 1 ? page : 1
}
