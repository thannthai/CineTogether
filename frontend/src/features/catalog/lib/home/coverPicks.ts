import type { MovieSummary } from '../../api/types'

const keyOf = (movie: MovieSummary) => `${movie.providerKey}/${movie.movieId}`

// One cover movie per theme, in order: a theme takes the first movie of its own list that has a poster and was not
// taken by an earlier theme, preferring one released this year. A theme with nothing usable gets null.
export function pickDistinct(lists: MovieSummary[][], currentYear: number): (MovieSummary | null)[] {
  const taken = new Set<string>()
  return lists.map((list) => {
    const usable = list.filter((movie) => movie.posterUrl && !taken.has(keyOf(movie)))
    const pick = usable.find((movie) => (movie.releaseYear ?? 0) >= currentYear) ?? usable[0] ?? null
    if (pick) taken.add(keyOf(pick))
    return pick
  })
}
