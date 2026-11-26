import { Link } from 'react-router-dom'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { getMovieCached } from '../api'
import { moviePath, typeLabel, watchPath } from '../moviePaths'
import { PREVIEW_WIDTH, previewPosition } from '../previewPosition'
import type { MovieSummary } from '../types'
import type { Anchor } from './useHoverPreview'

type Props = {
  movie: MovieSummary
  anchor: Anchor
  onEnter: () => void
  onLeave: () => void
  onNavigate: () => void
}

export function MoviePreview({ movie, anchor, onEnter, onLeave, onNavigate }: Props) {
  const openSoon = useSoon()
  const detail = useAsync(() => getMovieCached(movie.providerKey, movie.movieId), `preview:${movie.providerKey}/${movie.movieId}`)
  const { left, top } = previewPosition(anchor, { width: window.innerWidth, height: window.innerHeight })
  const media = detail.status === 'ready' ? (detail.data.backdropUrl ?? movie.posterUrl) : null
  const synopsis = detail.status === 'ready' ? detail.data.synopsis : null
  const genres = movie.genres ?? []

  return (
    <div
      className="movie-preview"
      style={{ left, top, width: PREVIEW_WIDTH }}
      onMouseEnter={onEnter}
      onMouseLeave={onLeave}
    >
      <div className="preview-media">
        {media ? <img src={media} alt="" /> : <span className="skeleton" />}
      </div>
      <div className="preview-body">
        <h3>{movie.title}</h3>
        <p className="preview-meta">
          {movie.releaseYear ? <span>{movie.releaseYear}</span> : null}
          <span className="type-badge">{typeLabel(movie.type)}</span>
        </p>
        {genres.length > 0 && <p className="preview-genres">{genres.slice(0, 3).join(' • ')}</p>}
        {detail.status === 'loading' ? (
          <div className="preview-synopsis-skeleton" aria-hidden="true">
            <span className="skeleton" />
            <span className="skeleton" />
          </div>
        ) : (
          synopsis && <p className="preview-synopsis">{synopsis}</p>
        )}
        <div className="preview-actions">
          <Link className="btn primary" to={watchPath(movie)} onClick={onNavigate}>
            <Icon name="play" size={14} /> Xem ngay
          </Link>
          <button
            className="btn secondary"
            onClick={() => {
              onNavigate()
              openSoon()
            }}
          >
            <Icon name="people" size={15} /> Xem cùng <SoonTag />
          </button>
          <Link className="circle-btn" to={moviePath(movie)} aria-label="Chi tiết phim" title="Chi tiết" onClick={onNavigate}>
            <Icon name="info" size={16} />
          </Link>
        </div>
      </div>
    </div>
  )
}
