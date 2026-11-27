import type { MovieSummary } from './types'

// This year's movies first; last year's only fill what is missing up to `size`, without repeating a movie.
export function fillFromPreviousYear(current: MovieSummary[], previous: MovieSummary[], size: number): MovieSummary[] {
  const seen = new Set(current.map((movie) => movie.movieId))
  const fill = previous.filter((movie) => !seen.has(movie.movieId))
  return [...current, ...fill].slice(0, size)
}
