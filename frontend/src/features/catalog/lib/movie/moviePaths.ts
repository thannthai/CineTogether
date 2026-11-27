import type { MovieType } from '../../api/types'

type MovieRef = { providerKey: string; movieId: string }

export const moviePath = ({ providerKey, movieId }: MovieRef) =>
  `/movies/${encodeURIComponent(providerKey)}/${encodeURIComponent(movieId)}`

export const watchPath = (movie: MovieRef, episodeId?: string) =>
  `${moviePath(movie)}/watch${episodeId ? `?episode=${encodeURIComponent(episodeId)}` : ''}`

export const typeLabel = (type: MovieType) => (type === 'SERIES' ? 'Phim bộ' : 'Phim lẻ')
