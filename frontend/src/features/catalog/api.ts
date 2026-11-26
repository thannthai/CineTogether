import { apiGet } from '../../shared/lib/api'
import type { BrowseFilters } from './browseFilters'
import type { Home, MovieDetail, MoviePage } from './types'

export const getHome = (signal?: AbortSignal) => apiGet<Home>('/catalog/home', {}, signal)

// `page` starts at 0, as in the API.
export const browseMovies = (
  filters: BrowseFilters,
  paging: { page?: number; size?: number } = {},
  signal?: AbortSignal,
) => apiGet<MoviePage>('/movies', { ...filters, ...paging }, signal)

export const getMovie = (providerKey: string, movieId: string, signal?: AbortSignal) =>
  apiGet<MovieDetail>(
    `/movies/${encodeURIComponent(providerKey)}/${encodeURIComponent(movieId)}`,
    {},
    signal,
  )
