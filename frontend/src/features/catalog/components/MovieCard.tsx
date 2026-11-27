import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import { differentTitle } from '../lib/movie/movieLabels'
import { moviePath } from '../lib/movie/moviePaths'
import type { MovieSummary } from '../api/types'
import { MovieTags } from './MovieTags'
import { MoviePreview } from './MoviePreview'
import { useHoverPreview } from '../hooks/useHoverPreview'

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
          <MovieTags
            className="poster-tags"
            part="badges"
            quality={movie.quality}
            type={movie.type}
          />
          <MovieTags className="poster-progress" part="progress" progress={movie.progress} type={movie.type} />
        </div>
        {movie.releaseYear ? <p className="card-year">{movie.releaseYear}</p> : null}
        <h3>{movie.title}</h3>
        {differentTitle(movie) && <p className="original-title">{movie.originalTitle}</p>}
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
