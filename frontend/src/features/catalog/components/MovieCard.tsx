import { Link } from 'react-router-dom'
import { moviePath, typeLabel } from '../moviePaths'
import type { MovieSummary } from '../types'

export function MovieCard({ movie }: { movie: MovieSummary }) {
  return (
    <Link className="movie-card" to={moviePath(movie)}>
      <div className="poster-wrap">
        {movie.posterUrl ? (
          <img src={movie.posterUrl} alt="" loading="lazy" />
        ) : (
          <div className="poster-fallback" aria-hidden="true">
            {movie.title.slice(0, 1)}
          </div>
        )}
      </div>
      <h3>{movie.title}</h3>
      <p>
        {movie.releaseYear ? <span>{movie.releaseYear}</span> : null}
        <span>{typeLabel(movie.type)}</span>
      </p>
    </Link>
  )
}

export function MovieCardSkeleton() {
  return (
    <div className="movie-card skeleton-card" aria-hidden="true">
      <div className="poster-wrap skeleton" />
      <div className="skeleton skeleton-line" />
      <div className="skeleton skeleton-line short" />
    </div>
  )
}
