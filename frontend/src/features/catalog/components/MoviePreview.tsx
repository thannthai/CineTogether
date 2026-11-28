import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { getMovieCached } from '../api/catalogApi'
import { differentTitle, youtubeId } from '../lib/movie/movieLabels'
import { moviePath, watchPath } from '../lib/movie/moviePaths'
import { PREVIEW_WIDTH, previewPosition } from '../lib/movie/previewPosition'
import type { MovieSummary } from '../api/types'
import { MovieTags } from './MovieTags'
import type { Anchor } from '../hooks/useHoverPreview'

type Props = {
  movie: MovieSummary
  anchor: Anchor
  onEnter: () => void
  onLeave: () => void
  onNavigate: () => void
}

// The trailer plays muted inside the popup. The embed does not take the pointer, so a click anywhere on the popup
// still opens the detail page.
function trailerSrc(id: string) {
  const params = new URLSearchParams({
    autoplay: '1',
    mute: '1',
    controls: '0',
    loop: '1',
    playlist: id,
    modestbranding: '1',
    playsinline: '1',
    rel: '0',
    disablekb: '1',
    fs: '0',
    iv_load_policy: '3',
    cc_load_policy: '0',
  })
  return `https://www.youtube-nocookie.com/embed/${id}?${params}`
}

export function MoviePreview({ movie, anchor, onEnter, onLeave, onNavigate }: Props) {
  const openSoon = useSoon()
  const navigate = useNavigate()
  const [trailerReady, setTrailerReady] = useState(false)
  const detail = useAsync(
    () => getMovieCached(movie.providerKey, movie.movieId),
    `preview:${movie.providerKey}/${movie.movieId}`,
  )
  const { left, top } = previewPosition(anchor, { width: window.innerWidth, height: window.innerHeight })
  const data = detail.status === 'ready' ? detail.data : null
  const media = data ? (data.backdropUrl ?? movie.posterUrl) : null
  const trailer = youtubeId(data?.trailerUrl)
  const genres = movie.genres ?? []
  const progress = data?.progress ?? movie.progress

  return (
    <div
      className="movie-preview"
      style={{ left, top, width: PREVIEW_WIDTH }}
      onMouseEnter={onEnter}
      onMouseLeave={onLeave}
      onClick={(event) => {
        // Buttons and links inside keep their own action.
        if ((event.target as HTMLElement).closest('a, button')) return
        onNavigate()
        navigate(moviePath(movie))
      }}
    >
      <div className="preview-media">
        {media ? <img src={media} alt="" /> : <span className="skeleton" />}
        {trailer && (
          <iframe
            className={`preview-trailer ${trailerReady ? 'ready' : ''}`}
            src={trailerSrc(trailer)}
            title={`Trailer ${movie.title}`}
            allow="autoplay; encrypted-media"
            referrerPolicy="strict-origin-when-cross-origin"
            tabIndex={-1}
            onLoad={() => setTrailerReady(true)}
          />
        )}
      </div>
      <div className="preview-body">
        <h3>{movie.title}</h3>
        {differentTitle(movie) && <p className="preview-original">{movie.originalTitle}</p>}
        <MovieTags quality={movie.quality} language={movie.language} progress={progress} type={movie.type} />
        <p className="preview-meta">
          {movie.releaseYear ? <span>{movie.releaseYear}</span> : null}
          {data?.duration && <span>{data.duration}</span>}
        </p>
        {genres.length > 0 && <p className="preview-genres">{genres.slice(0, 3).join(' • ')}</p>}
        {detail.status === 'loading' ? (
          <div className="preview-synopsis-skeleton" aria-hidden="true">
            <span className="skeleton" />
            <span className="skeleton" />
          </div>
        ) : (
          data?.synopsis && <p className="preview-synopsis">{data.synopsis}</p>
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
