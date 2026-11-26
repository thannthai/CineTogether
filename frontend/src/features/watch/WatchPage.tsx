import { useEffect } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ApiError, errorMessage } from '../../shared/lib/apiError'
import { useAsync } from '../../shared/lib/useAsync'
import { Icon } from '../../shared/ui/Icon'
import { useSoon } from '../../shared/ui/soonContext'
import { SoonTag } from '../../shared/ui/SoonTag'
import { useAuth, useAuthDialog } from '../auth'
import { getMovieCached, moviePath, watchPath } from '../catalog'
import { Playback } from './Playback'
import { neighbours, pickEpisode } from './pickEpisode'
import './watch.css'

export function WatchPage() {
  const { providerKey = '', movieId = '' } = useParams()
  const [params] = useSearchParams()
  const auth = useAuth()
  const { openLogin, openRegister } = useAuthDialog()
  const openSoon = useSoon()
  const navigate = useNavigate()
  const detail = useAsync(() => getMovieCached(providerKey, movieId), `watch:${providerKey}/${movieId}`)

  const movie = detail.status === 'ready' ? detail.data : null
  const picked = movie ? pickEpisode(movie.episodes, params.get('episode')) : null
  const several = (movie?.episodes.length ?? 0) > 1
  const label = movie && picked && several ? `${movie.title} - ${picked.episode.title}` : movie?.title

  useEffect(() => {
    if (!label) return
    document.title = `${label} - CineTogether`
    return () => {
      document.title = 'CineTogether'
    }
  }, [label])

  if (detail.status === 'loading') {
    return (
      <section className="watch page-width" aria-hidden="true">
        <div className="player-box skeleton" />
      </section>
    )
  }

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

  const current = detail.data
  const around = picked ? neighbours(current.episodes, picked.index) : null
  const goTo = (episode: { episodeId: string } | null | undefined) =>
    episode ? () => navigate(watchPath(current, episode.episodeId)) : null
  const episodeNav = current.episodes.length > 1 ? { previous: goTo(around?.previous), next: goTo(around?.next) } : null
  return (
    <section className="watch page-width">
      <Link className="back-link" to={moviePath(current)}>
        <Icon name="arrow" size={14} className="flip" /> {current.title}
      </Link>

      <div className="player-box">
        {!picked ? (
          <div className="player-state">
            <Icon name="alert" size={26} />
            <h2>Chưa có tập phim</h2>
            <p>Phim này chưa có tập nào để phát.</p>
          </div>
        ) : auth.status === 'loading' ? (
          <div className="player-state" role="status">
            <span className="spinner" aria-hidden="true" />
          </div>
        ) : auth.status === 'signedOut' ? (
          <div className="player-state">
            <Icon name="user" size={26} />
            <h2>Đăng nhập để xem phim</h2>
            <p>Bạn cần có tài khoản để phát phim. Đăng ký chỉ mất vài giây.</p>
            <div className="player-actions">
              <button className="btn primary" onClick={openLogin}>
                Đăng nhập
              </button>
              <button className="btn secondary" onClick={openRegister}>
                Đăng ký
              </button>
            </div>
          </div>
        ) : (
          <Playback
            key={`${current.movieId}/${picked.episode.episodeId}`}
            providerKey={current.providerKey}
            movieId={current.movieId}
            episodeId={picked.episode.episodeId}
            poster={current.backdropUrl ?? current.posterUrl}
            episodeNav={episodeNav}
          />
        )}
      </div>

      <div className="watch-info">
        <div>
          <h1>{current.title}</h1>
          {picked && several && <p className="now-playing">Đang xem: {picked.episode.title}</p>}
        </div>
        <button className="btn secondary" onClick={openSoon}>
          <Icon name="people" size={16} /> Xem cùng <SoonTag />
        </button>
      </div>

      {several && (
        <section className="episodes" aria-labelledby="watch-episodes">
          <div className="row-heading">
            <h2 id="watch-episodes">Danh sách tập</h2>
            <span className="episodes-count">{current.episodes.length} tập</span>
          </div>
          <div className="episode-grid">
            {current.episodes.map((episode) => (
              <Link
                key={episode.episodeId}
                to={watchPath(current, episode.episodeId)}
                className={picked?.episode.episodeId === episode.episodeId ? 'current' : ''}
                aria-current={picked?.episode.episodeId === episode.episodeId ? 'true' : undefined}
              >
                {episode.title}
              </Link>
            ))}
          </div>
        </section>
      )}
    </section>
  )
}
