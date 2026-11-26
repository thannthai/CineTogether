import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import { moviePath, typeLabel } from '../moviePaths'
import type { MovieSummary } from '../types'
import { MoviePreview } from './MoviePreview'
import { useHoverPreview } from './useHoverPreview'

export function MovieCard({ movie }: { movie: MovieSummary }) {
  const preview = useHoverPreview()
  return (
    <>
      <Link
        className="movie-card"
        to={moviePath(movie)}
        onMouseEnter={(event) =>
          preview.open(event.currentTarget.querySelector<HTMLElement>('.poster-wrap') ?? event.currentTarget)
        }
        onMouseLeave={preview.scheduleClose}
        onClick={preview.close}
      >
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
      {preview.anchor &&
        createPortal(
          <MoviePreview
            movie={movie}
            anchor={preview.anchor}
            onEnter={preview.keepOpen}
            onLeave={preview.scheduleClose}
            onNavigate={preview.close}
          />,
          document.body,
        )}
    </>
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
