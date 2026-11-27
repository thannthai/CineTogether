import { Icon } from '../../../shared/ui/Icon'
import { SoonTag } from '../../../shared/ui/SoonTag'
import { useSoon } from '../../../shared/ui/soonContext'
import '../party.css'

// Home banner. Rooms are not available yet, so both actions open the "coming soon" dialog.
export function WatchTogetherStrip() {
  const openSoon = useSoon()
  return (
    <section className="party-strip page-width" aria-label="Xem chung">
      <div className="party-strip-copy">
        <span className="party-strip-eyebrow">
          Kết nối qua từng thước phim <SoonTag />
        </span>
        <h2>Xem phim cùng bạn bè</h2>
        <p>Cùng xem, cùng cảm xúc trong phòng chiếu của riêng bạn.</p>
      </div>
      <div className="party-strip-actions">
        <button className="party-action" onClick={openSoon}>
          <span className="party-action-icon">
            <Icon name="plus" size={18} />
          </span>
          <span>
            <strong>Tạo phòng</strong>
            <small>Tạo phòng riêng và mời bạn bè.</small>
          </span>
          <Icon name="arrow" size={16} />
        </button>
        <button className="party-action" onClick={openSoon}>
          <span className="party-action-icon">
            <Icon name="link" size={18} />
          </span>
          <span>
            <strong>Tham gia phòng</strong>
            <small>Bạn có lời mời? Nhập liên kết hoặc mã phòng.</small>
          </span>
          <Icon name="arrow" size={16} />
        </button>
      </div>
    </section>
  )
}
