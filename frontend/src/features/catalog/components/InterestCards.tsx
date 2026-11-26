import { Link } from 'react-router-dom'
import { browseHref } from '../browseFilters'

type Tone = 'sky' | 'butter' | 'pink' | 'lavender' | 'mint'

// Each card opens the browse page with one filter. Anime is left out until the backend can list it.
const INTERESTS: { tag: string; title: string; subtitle: string; to: string }[] = [
  { tag: 'Gia đình', title: 'Gia đình', subtitle: 'Cả nhà cùng xem', to: browseHref({ genre: 'gia-dinh' }) },
  { tag: 'Hàn Quốc', title: 'Phim Hàn', subtitle: 'Lãng mạn và kịch tính', to: browseHref({ country: 'han-quoc' }) },
  { tag: 'Cổ trang', title: 'Cổ trang', subtitle: 'Kiếm hiệp và cung đấu', to: browseHref({ genre: 'co-trang' }) },
  { tag: 'Hành động', title: 'Hành động', subtitle: 'Nghẹt thở từng phút', to: browseHref({ genre: 'hanh-dong' }) },
  { tag: 'Tình cảm', title: 'Tình cảm', subtitle: 'Những câu chuyện ngọt ngào', to: browseHref({ genre: 'tinh-cam' }) },
  { tag: 'Kinh dị', title: 'Kinh dị', subtitle: 'Dành cho người can đảm', to: browseHref({ genre: 'kinh-di' }) },
  { tag: 'Viễn tưởng', title: 'Viễn tưởng', subtitle: 'Vượt ngoài trí tưởng tượng', to: browseHref({ genre: 'vien-tuong' }) },
  { tag: 'Nhật Bản', title: 'Phim Nhật', subtitle: 'Tinh tế và sâu lắng', to: browseHref({ country: 'nhat-ban' }) },
]

const TONES: Tone[] = ['sky', 'butter', 'pink', 'lavender', 'mint']

export function InterestCards() {
  return (
    <section className="interests page-width" aria-labelledby="interests-title">
      <div className="row-heading">
        <h2 id="interests-title">Bạn đang quan tâm gì?</h2>
        <Link className="see-all" to={browseHref()}>
          Khám phá tất cả
        </Link>
      </div>
      <div className="interest-row">
        {INTERESTS.map((item, i) => (
          <Link key={item.to} to={item.to} className={`interest-card tone-${TONES[i % TONES.length]}`}>
            <span className="interest-tag">{item.tag}</span>
            <span className="interest-copy">
              <strong>{item.title}</strong>
              <small>{item.subtitle}</small>
            </span>
          </Link>
        ))}
      </div>
    </section>
  )
}
