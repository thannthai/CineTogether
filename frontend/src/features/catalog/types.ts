// Shapes of the catalog responses in docs/design/api.md §2.2. Optional fields may be missing or null.
export type MovieType = 'MOVIE' | 'SERIES'

export type MovieSummary = {
  providerKey: string
  movieId: string
  title: string
  type: MovieType
  posterUrl?: string | null
  releaseYear?: number | null
  genres?: string[] | null
}

export type Episode = { episodeId: string; title: string }

export type MovieDetail = {
  providerKey: string
  movieId: string
  title: string
  type: MovieType
  synopsis?: string | null
  posterUrl?: string | null
  backdropUrl?: string | null
  genres?: string[] | null
  episodes: Episode[]
}

export type MoviePage = { items: MovieSummary[]; page: number; size: number; hasNext: boolean }

export type Home = {
  featured: MovieSummary[]
  sections: { key: string; title: string; items: MovieSummary[] }[]
}
