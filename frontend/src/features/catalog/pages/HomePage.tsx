import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync, type AsyncState } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { WatchTogetherStrip } from '../../party'
import { browseMovies, getHome } from '../api'
import { browseHref, type BrowseFilters } from '../browseFilters'
import { Hero, HeroSkeleton } from '../components/Hero'
import { InterestCards } from '../components/InterestCards'
import { MovieRow } from '../components/MovieRow'
import type { MovieSummary } from '../types'
import '../catalog.css'

const ROW_SIZE = 14

// One independent row: a failure in one provider call never blanks the rest of the page.
function BrowseRow({ title, filters }: { title: string; filters: BrowseFilters }) {
  const key = JSON.stringify(filters)
  const state = useAsync(
    async (signal) => (await browseMovies(filters, { size: ROW_SIZE }, signal)).items,
    `row:${key}`,
  )
  return <MovieRow title={title} seeAllTo={browseHref(filters)} state={state} onRetry={state.reload} />
}

export function HomePage() {
  const home = useAsync(getHome, 'home')

  const newUpdates: AsyncState<MovieSummary[]> =
    home.status === 'ready'
      ? { status: 'ready', data: home.data.sections.find((s) => s.key === 'new-updates')?.items ?? [] }
      : home.status === 'error'
        ? { status: 'error', error: home.error }
        : { status: 'loading' }

  return (
    <>
      {home.status === 'loading' && <HeroSkeleton />}
      {home.status === 'ready' && home.data.featured.length > 0 && <Hero featured={home.data.featured} />}
      {home.status === 'error' && (
        <section className="home-error page-width" role="alert">
          <Icon name="alert" size={22} />
          <p>{errorMessage(home.error)}</p>
          <button className="btn secondary" onClick={home.reload}>
            Thử lại
          </button>
        </section>
      )}

      <InterestCards />
      <WatchTogetherStrip />

      {home.status !== 'error' && (
        <MovieRow title="Phim mới cập nhật" seeAllTo={browseHref()} state={newUpdates} onRetry={home.reload} />
      )}
      <BrowseRow title="Phim lẻ mới cập nhật" filters={{ type: 'MOVIE' }} />
      <BrowseRow title="Phim bộ mới cập nhật" filters={{ type: 'SERIES' }} />
      <BrowseRow title="Dành cho gia đình" filters={{ genre: 'gia-dinh' }} />
    </>
  )
}
