import { useState } from 'react'
import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync, type AsyncState } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { WatchTogetherStrip } from '../../party'
import { browseMovies, getHome } from '../api/catalogApi'
import { browseHref, type BrowseFilters } from '../lib/browse/browseFilters'
import { Hero, HeroSkeleton } from '../components/Hero'
import { InterestCards } from '../components/InterestCards'
import { MovieRow } from '../components/MovieRow'
import { fillFromPreviousYear } from '../lib/home/recentYears'
import type { MovieSummary } from '../api/types'
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

// A row of the movies of this year; when there are fewer than a full row, last year's fill the rest. The year is read
// at run time, so nothing has to change when a new year starts.
function RecentRow({ title, filters }: { title: string; filters: BrowseFilters }) {
  const [year] = useState(() => new Date().getFullYear())
  const state = useAsync(async (signal) => {
    const current = (await browseMovies({ ...filters, year }, { size: ROW_SIZE }, signal)).items
    if (current.length >= ROW_SIZE) return current
    const previous = (await browseMovies({ ...filters, year: year - 1 }, { size: ROW_SIZE }, signal)).items
    return fillFromPreviousYear(current, previous, ROW_SIZE)
  }, `recent:${year}:${JSON.stringify(filters)}`)
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

  // This year's most viewed cinema films, picked by the backend. There is no row when the section is missing.
  const cinema = home.status === 'ready' ? home.data.sections.find((section) => section.key === 'cinema') : undefined

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
      {cinema && (
        <MovieRow
          title={cinema.title}
          seeAllTo={browseHref({ list: 'CINEMA' })}
          state={{ status: 'ready', data: cinema.items }}
          onRetry={home.reload}
        />
      )}
      <RecentRow title="Dành cho gia đình" filters={{ genre: 'gia-dinh' }} />
    </>
  )
}
