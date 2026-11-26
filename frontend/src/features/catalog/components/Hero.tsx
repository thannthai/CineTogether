import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { getMovie } from '../api'
import { moviePath, typeLabel, watchPath } from '../moviePaths'
import type { MovieSummary } from '../types'

// The featured movies come from the home response. The list has no synopsis, so the selected
// movie's detail is loaded for it, and it also carries the landscape image used as the background.
export function Hero({ featured }: { featured: MovieSummary[] }) {
  const [index, setIndex] = useState(0)
  const openSoon = useSoon()
  const movie = featured[index]
  const detail = useAsync((signal) => getMovie(movie.providerKey, movie.movieId, signal), `hero:${movie.providerKey}/${movie.movieId}`)
  const genres = movie.genres ?? []
  const synopsis = detail.status === 'ready' ? detail.data.synopsis : null
  // The poster is portrait and looks bad stretched, so it is only a fallback when there is no landscape image.
  const background =
    detail.status === 'ready' ? (detail.data.backdropUrl ?? movie.posterUrl) : detail.status === 'error' ? movie.posterUrl : null

  return (
    <section className="hero" aria-label="Phim nổi bật">
      {background && <img key={background} className="hero-backdrop" src={background} alt="" />}
      <div className="hero-scrim" />
      <div className="hero-inner page-width">
        <div className="hero-copy">
          <h1>{movie.title}</h1>
          <p className="hero-meta">
            {movie.releaseYear ? <span>{movie.releaseYear}</span> : null}
            <span className="type-badge">{typeLabel(movie.type)}</span>
          </p>
          {genres.length > 0 && (
            <ul className="hero-genres">
              {genres.slice(0, 4).map((genre) => (
                <li key={genre}>{genre}</li>
              ))}
            </ul>
          )}
          {detail.status === 'loading' ? (
            <div className="hero-synopsis-skeleton" aria-hidden="true">
              <span className="skeleton" />
              <span className="skeleton" />
              <span className="skeleton short" />
            </div>
          ) : (
            synopsis && <p className="hero-synopsis">{synopsis}</p>
          )}
          <div className="hero-actions">
            <Link className="btn primary large" to={watchPath(movie)}>
              <Icon name="play" size={16} /> Xem ngay
            </Link>
            <button className="btn glass large" onClick={openSoon}>
              <Icon name="people" size={17} /> Xem cùng <SoonTag />
            </button>
            <Link className="circle-btn glass" to={moviePath(movie)} aria-label="Chi tiết phim" title="Chi tiết">
              <Icon name="info" size={18} />
            </Link>
          </div>
        </div>

        <div className="hero-picker" role="tablist" aria-label="Chọn phim nổi bật">
          {featured.map((item, i) => (
            <button
              key={`${item.providerKey}/${item.movieId}`}
              role="tab"
              aria-selected={i === index}
              aria-label={item.title}
              className={`hero-thumb ${i === index ? 'selected' : ''}`}
              onClick={() => setIndex(i)}
            >
              {item.posterUrl ? <img src={item.posterUrl} alt="" /> : <span>{item.title.slice(0, 1)}</span>}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}

export function HeroSkeleton() {
  return (
    <section className="hero hero-loading" aria-hidden="true">
      <div className="hero-inner page-width">
        <div className="hero-copy">
          <span className="skeleton hero-skeleton-title" />
          <span className="skeleton hero-skeleton-line" />
          <span className="skeleton hero-skeleton-line short" />
        </div>
      </div>
    </section>
  )
}
