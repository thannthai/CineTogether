import { Link } from 'react-router-dom'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { browseMovies } from '../api/catalogApi'
import { browseHref, type BrowseFilters } from '../lib/browse/browseFilters'
import { pickDistinct } from '../lib/home/coverPicks'
import type { MovieSummary } from '../api/types'

type Tone = 'sky' | 'butter' | 'pink' | 'lavender' | 'mint'

// Each card opens the browse page with one filter. Anime is left out until the backend can list it.
const INTERESTS: { title: string; filters: BrowseFilters }[] = [
  { title: 'Gia đình', filters: { genre: 'gia-dinh' } },
  { title: 'Phim Hàn', filters: { country: 'han-quoc' } },
  { title: 'Cổ trang', filters: { genre: 'co-trang' } },
  { title: 'Hành động', filters: { genre: 'hanh-dong' } },
  { title: 'Tình cảm', filters: { genre: 'tinh-cam' } },
  { title: 'Kinh dị', filters: { genre: 'kinh-di' } },
  { title: 'Viễn tưởng', filters: { genre: 'vien-tuong' } },
  { title: 'Phim Nhật', filters: { country: 'nhat-ban' } },
]

// Used while the covers load and for a theme without one.
const TONES: Tone[] = ['sky', 'butter', 'pink', 'lavender', 'mint']

const CANDIDATES = 8
const YEAR = new Date().getFullYear()

// The cover of a theme is a recently updated movie of that theme (the API has no popularity), never the same
// movie twice. A theme whose request fails keeps its flat pastel color.
function useCovers() {
  const state = useAsync(async (signal) => {
    const results = await Promise.allSettled(
      INTERESTS.map((interest) => browseMovies(interest.filters, { size: CANDIDATES }, signal)),
    )
    const lists = results.map<MovieSummary[]>((result) => (result.status === 'fulfilled' ? result.value.items : []))
    return pickDistinct(lists, YEAR)
  }, 'interest-covers')
  return state.status === 'ready' ? state.data : null
}

export function InterestCards() {
  const covers = useCovers()
  return (
    <section className="interests page-width" aria-labelledby="interests-title">
      <div className="row-heading">
        <h2 id="interests-title">Bạn đang quan tâm gì?</h2>
        <Link className="see-all" to={browseHref()}>
          Khám phá tất cả <Icon name="arrow" size={14} />
        </Link>
      </div>
      <div className="interest-row">
        {INTERESTS.map((item, i) => {
          const cover = covers?.[i]
          return (
            <Link
              key={item.title}
              to={browseHref(item.filters)}
              className={`interest-card ${cover ? 'has-cover' : `tone-${TONES[i % TONES.length]}`}`}
              draggable={false}
            >
              {cover?.posterUrl && (
                <img className="interest-cover" src={cover.posterUrl} alt="" loading="lazy" draggable={false} />
              )}
              <span className="interest-copy">
                <strong>{item.title}</strong>
                <small>
                  Khám phá <Icon name="arrow" size={13} />
                </small>
              </span>
            </Link>
          )
        })}
      </div>
    </section>
  )
}
