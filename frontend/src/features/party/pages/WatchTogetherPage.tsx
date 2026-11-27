import { useState } from 'react'
import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { browseMovies, getMovieCached } from '../../catalog'
import { mockRooms, type RoomStatus } from '../lib/mockRooms'
import { filterRooms, type RoomTab } from '../lib/roomFilter'
import { joinNote, movieLine } from '../lib/roomText'
import '../party.css'

const TABS: { key: RoomTab; label: string }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'live', label: 'Đang xem' },
  { key: 'upcoming', label: 'Sắp bắt đầu' },
]

const STATUS_LABEL: Record<RoomStatus, string> = { live: 'Đang xem', upcoming: 'Sắp bắt đầu', ended: 'Đã kết thúc' }

// Watch Together is announced, not built: every action opens the "coming soon" dialog and the rooms are samples.
export function WatchTogetherPage() {
  const openSoon = useSoon()
  const [tab, setTab] = useState<RoomTab>('all')
  const [query, setQuery] = useState('')
  // The sample rooms play real movies; the wide cover is the movie's landscape image, which only its detail has.
  // A detail that fails just leaves that room with the poster as its cover.
  const movies = useAsync(async (signal) => {
    const items = (await browseMovies({}, { size: 6 }, signal)).items
    const details = await Promise.all(items.map((movie) => getMovieCached(movie.providerKey, movie.movieId).catch(() => null)))
    const covers = new Map(items.map((movie, i) => [movie.movieId, details[i]?.backdropUrl ?? null]))
    return { items, covers }
  }, 'together-sample')
  const rooms = filterRooms(movies.status === 'ready' ? mockRooms(movies.data.items) : [], tab, query)

  return (
    <section className="together page-width">
      <header className="together-head">
        <div>
          <p className="together-eyebrow">Một bộ phim, thêm những người bạn</p>
          <h1 className="together-title">Khám phá phòng công khai</h1>
          <p className="together-lead">Tham gia những buổi xem phim cùng cộng đồng CineTogether.</p>
        </div>
        <div className="together-actions">
          <button className="btn primary" onClick={openSoon}>
            <Icon name="plus" size={16} /> Tạo phòng
          </button>
          <button className="btn secondary" onClick={openSoon}>
            <Icon name="link" size={16} /> Tham gia bằng link
          </button>
        </div>
      </header>

      <div className="room-toolbar">
        <div className="room-tabs" role="tablist" aria-label="Lọc phòng theo trạng thái">
          {TABS.map(({ key, label }) => (
            <button key={key} role="tab" aria-selected={tab === key} className={tab === key ? 'selected' : ''} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </div>
        <label className="room-search">
          <Icon name="search" size={17} />
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Tìm phòng xem chung..."
            aria-label="Tìm phòng"
          />
        </label>
      </div>

      {movies.status === 'error' && (
        <div className="load-error" role="alert">
          <Icon name="alert" size={18} />
          <p>{errorMessage(movies.error)}</p>
          <button onClick={movies.reload}>
            Thử lại <Icon name="refresh" size={14} />
          </button>
        </div>
      )}
      {movies.status === 'loading' && (
        <div className="room-grid" aria-hidden="true">
          {Array.from({ length: 6 }, (_, i) => (
            <span key={i} className="skeleton room-skeleton" />
          ))}
        </div>
      )}
      {movies.status === 'ready' && (
        <>
          <p className="room-count">{rooms.length} phòng</p>
          {rooms.length === 0 ? (
            <p className="room-empty">Không có phòng nào khớp. Thử đổi bộ lọc hoặc từ khoá khác.</p>
          ) : (
            <div className="room-grid">
              {rooms.map((room) => (
                <button key={room.id} className={`room-card ${room.status}`} onClick={openSoon}>
                  <span className="room-cover">
                    {(movies.data.covers.get(room.movie.movieId) ?? room.movie.posterUrl) && (
                      <img src={movies.data.covers.get(room.movie.movieId) ?? room.movie.posterUrl ?? ''} alt="" loading="lazy" />
                    )}
                    <span className="room-state">
                      {room.status === 'live' && <i aria-hidden="true" />}
                      {STATUS_LABEL[room.status]}
                    </span>
                    <span className="room-movie">
                      {room.movie.posterUrl && <img className="room-poster" src={room.movie.posterUrl} alt="" loading="lazy" />}
                      <span className="room-movie-text">
                        <strong>{room.movie.title}</strong>
                        <em>{movieLine(room)}</em>
                      </span>
                    </span>
                  </span>
                  <span className="room-body">
                    <strong className="room-name">{room.name}</strong>
                    <span className="room-meta">
                      <span className="room-host">
                        <span className="room-crown">
                          <Icon name="crown" size={15} />
                        </span>
                        <span>
                          <b>{room.hostDisplayName}</b>
                          <small>Chủ phòng</small>
                        </span>
                      </span>
                      <span className="room-people">
                        <Icon name="people" size={16} />
                        {room.participantCount}/{room.capacity}
                      </span>
                    </span>
                    <span className="room-foot">
                      <span className={room.available ? 'open' : ''}>{joinNote(room)}</span>
                      <span className="room-go">
                        Xem phòng <Icon name="arrow" size={14} />
                      </span>
                    </span>
                  </span>
                </button>
              ))}
            </div>
          )}
        </>
      )}
      <p className="together-note">Dữ liệu minh họa</p>
    </section>
  )
}
