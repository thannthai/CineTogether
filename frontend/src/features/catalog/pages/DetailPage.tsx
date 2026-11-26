import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, errorMessage } from '../../../shared/lib/apiError'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { browseMovies, getMovieCached } from '../api'
import { browseHref } from '../browseFilters'
import { MovieRow } from '../components/MovieRow'
import { genreSlug } from '../filters'
import { typeLabel, watchPath } from '../moviePaths'
import type { MovieDetail, MovieSummary } from '../types'
import '../catalog.css'

const RELATED_SIZE = 15

// Other movies of the first genre that the filters know, without the movie being shown.
function RelatedRow({ movie, genre }: { movie: MovieDetail; genre: string }) {
  const state = useAsync(
    async (signal) =>
      (await browseMovies({ genre }, { size: RELATED_SIZE }, signal)).items.filter(
        (item: MovieSummary) => item.movieId !== movie.movieId,
      ),
    `related:${movie.providerKey}/${movie.movieId}/${genre}`,
  )
  return <MovieRow title="Phim cùng thể loại" seeAllTo={browseHref({ genre })} state={state} onRetry={state.reload} />
}

function DetailSkeleton() {
  return (
    <section className="detail-hero" aria-hidden="true">
      <div className="detail-inner page-width">
        <span className="skeleton detail-poster-skeleton" />
        <div className="detail-copy">
          <span className="skeleton hero-skeleton-title" />
          <span className="skeleton hero-skeleton-line" />
          <span className="skeleton hero-skeleton-line short" />
        </div>
      </div>
    </section>
  )
}

export function DetailPage() {
  const { providerKey = '', movieId = '' } = useParams()
  const openSoon = useSoon()
  const detail = useAsync(() => getMovieCached(providerKey, movieId), `detail:${providerKey}/${movieId}`)
  const title = detail.status === 'ready' ? detail.data.title : null

  useEffect(() => {
    if (!title) return
    document.title = `${title} - CineTogether`
    return () => {
      document.title = 'CineTogether'
    }
  }, [title])

  if (detail.status === 'loading') return <DetailSkeleton />

  if (detail.status === 'error') {
    const notFound = detail.error instanceof ApiError && detail.error.status === 404
    return (
      <section className="status-page page-width" role="alert">
        <Icon name="alert" size={26} />
        <h1>{notFound ? 'Không tìm thấy phim' : 'Không tải được phim'}</h1>
        <p>{notFound ? 'Phim này không tồn tại hoặc đã được gỡ.' : errorMessage(detail.error)}</p>
        {notFound ? (
          <Link className="btn primary" to="/">
            Về trang chủ
          </Link>
        ) : (
          <button className="btn primary" onClick={detail.reload}>
            Thử lại
          </button>
        )}
      </section>
    )
  }

  const movie = detail.data
  const background = movie.backdropUrl ?? movie.posterUrl
  const genres = movie.genres ?? []
  const firstEpisode = movie.episodes[0]
  const related = genres.map(genreSlug).find((slug): slug is string => slug !== undefined)

  return (
    <>
      <section className="detail-hero">
        {background && <img className="hero-backdrop" src={background} alt="" />}
        <div className="hero-scrim" />
        <div className="detail-inner page-width">
          {movie.posterUrl && <img className="detail-poster" src={movie.posterUrl} alt={movie.title} />}
          <div className="detail-copy">
            <h1>{movie.title}</h1>
            <p className="hero-meta">
              <span className="type-badge">{typeLabel(movie.type)}</span>
              {movie.episodes.length > 1 && <span>{movie.episodes.length} tập</span>}
            </p>
            {genres.length > 0 && (
              <ul className="hero-genres">
                {genres.map((genre) => {
                  const slug = genreSlug(genre)
                  return (
                    <li key={genre}>{slug ? <Link to={browseHref({ genre: slug })}>{genre}</Link> : genre}</li>
                  )
                })}
              </ul>
            )}
            {movie.synopsis && <p className="detail-synopsis">{movie.synopsis}</p>}
            <div className="hero-actions">
              {firstEpisode ? (
                <Link className="btn primary large" to={watchPath(movie, firstEpisode.episodeId)}>
                  <Icon name="play" size={16} /> Xem ngay
                </Link>
              ) : (
                <button className="btn primary large" disabled>
                  Chưa có tập phim
                </button>
              )}
              <button className="btn glass large" onClick={openSoon}>
                <Icon name="people" size={17} /> Xem cùng <SoonTag />
              </button>
            </div>
          </div>
        </div>
      </section>

      {movie.episodes.length > 1 && (
        <section className="episodes page-width" aria-labelledby="episodes-title">
          <div className="row-heading">
            <h2 id="episodes-title">Danh sách tập</h2>
            <span className="episodes-count">{movie.episodes.length} tập</span>
          </div>
          <div className="episode-grid">
            {movie.episodes.map((episode) => (
              <Link key={episode.episodeId} to={watchPath(movie, episode.episodeId)}>
                {episode.title}
              </Link>
            ))}
          </div>
        </section>
      )}

      {related && <RelatedRow movie={movie} genre={related} />}
    </>
  )
}

