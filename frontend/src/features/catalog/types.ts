// Shapes of the catalog responses in docs/design/api.md §2.2. Optional fields may be missing or null.
export type MovieType = 'MOVIE' | 'SERIES'

// The latest episode, the planned total and whether a series is finished; the numbers are null when unknown.
export type EpisodeProgress = { current: number | null; total: number | null; completed: boolean }

export type MovieSummary = {
  providerKey: string
  movieId: string
  title: string
  originalTitle?: string | null
  type: MovieType
  posterUrl?: string | null
  releaseYear?: number | null
  genres?: string[] | null
  quality?: string | null
  language?: string | null
  progress?: EpisodeProgress | null
  // How many people rated it (IMDb, else TMDb): how well known it is. Not shown; the backend ranks the home page by it.
  ratingCount?: number | null
}

export type Episode = { episodeId: string; title: string }

export type MovieDetail = {
  providerKey: string
  movieId: string
  title: string
  originalTitle?: string | null
  type: MovieType
  synopsis?: string | null
  posterUrl?: string | null
  backdropUrl?: string | null
  genres?: string[] | null
  episodes: Episode[]
  releaseYear?: number | null
  quality?: string | null
  language?: string | null
  duration?: string | null
  progress?: EpisodeProgress | null
  actors?: string[] | null
  directors?: string[] | null
  trailerUrl?: string | null
}

export type MoviePage = { items: MovieSummary[]; page: number; size: number; hasNext: boolean }

export type Home = {
  featured: MovieSummary[]
  sections: { key: string; title: string; items: MovieSummary[] }[]
}
