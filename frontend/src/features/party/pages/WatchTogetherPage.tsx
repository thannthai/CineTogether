import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { useSoon } from '../../../shared/ui/soonContext'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { browseMovies, typeLabel } from '../../catalog'
import { mockRooms } from '../lib/mockRooms'
import '../party.css'

// Watch Together is announced, not built: every action opens the "coming soon" dialog and the rooms are samples.
export function WatchTogetherPage() {
  const openSoon = useSoon()
  const movies = useAsync(async (signal) => (await browseMovies({}, { size: 6 }, signal)).items, 'together-sample')
  const rooms = movies.status === 'ready' ? mockRooms(movies.data) : []

  return (
    <section className="together page-width">
      <div className="together-head">
        <h1>Xem phim cùng nhau</h1>
        <SoonTag />
      </div>
      <p className="together-lead">Tạo phòng riêng với bạn bè hoặc khám phá những buổi xem phim đang diễn ra.</p>

      <div className="together-actions">
        <article className="together-action">
          <span className="party-action-icon">
            <Icon name="plus" size={20} />
          </span>
          <div>
            <h2>Tạo phòng riêng</h2>
            <p>Tạo không gian xem phim riêng và mời bạn bè tham gia.</p>
          </div>
          <button className="btn primary" onClick={openSoon}>
            Tạo phòng
          </button>
        </article>
        <article className="together-action">
          <span className="party-action-icon">
            <Icon name="link" size={20} />
          </span>
          <div>
            <h2>Tham gia bằng link</h2>
            <p>Đã có lời mời? Nhập liên kết hoặc mã phòng để tham gia.</p>
          </div>
          <button className="btn secondary" onClick={openSoon}>
            Tham gia phòng <Icon name="arrow" size={14} />
          </button>
        </article>
      </div>

      <h2 className="together-title">Khám phá phòng công khai</h2>
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
      {rooms.length > 0 && (
        <div className="room-grid">
          {rooms.map((room) => (
            <button key={room.id} className="room-card" onClick={openSoon}>
              {room.movie.posterUrl && <img className="room-cover" src={room.movie.posterUrl} alt="" loading="lazy" />}
              <span className={`room-status ${room.available ? '' : 'full'}`}>
                {room.available ? 'Còn chỗ' : 'Phòng đã đầy'}
              </span>
              <span className="room-copy">
                <strong>{room.movie.title}</strong>
                <small>
                  {typeLabel(room.movie.type)} • Chủ phòng {room.hostDisplayName}
                </small>
                <span className="room-foot">
                  <span>
                    <Icon name="people" size={14} /> {room.participantCount}/{room.capacity}
                  </span>
                  <span>
                    Xem phòng <Icon name="arrow" size={13} />
                  </span>
                </span>
              </span>
            </button>
          ))}
        </div>
      )}
      <p className="together-note">Dữ liệu minh họa</p>
    </section>
  )
}
