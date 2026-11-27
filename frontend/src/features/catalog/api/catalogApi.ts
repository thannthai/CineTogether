import { apiGet } from '../../../shared/lib/api'
import { memoAsync } from '../../../shared/lib/memoAsync'
import type { BrowseFilters } from '../lib/browse/browseFilters'
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

// Detail of a movie remembered for this visit: the hover preview asks for it on every hover.
const detailOf = memoAsync((id: string) => {
  const [providerKey, movieId] = JSON.parse(id) as [string, string]
  return getMovie(providerKey, movieId)
})

export const getMovieCached = (providerKey: string, movieId: string) =>
  detailOf(JSON.stringify([providerKey, movieId]))
