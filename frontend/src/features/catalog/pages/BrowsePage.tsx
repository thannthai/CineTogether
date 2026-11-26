import { useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { browseMovies } from '../api'
import { browseHref, parseBrowseFilters, parsePage, type BrowseFilters } from '../browseFilters'
import { activeFilterCount, browseTitle } from '../browseView'
import { countriesParam, parseCountries, toggleCountry } from '../countrySelection'
import { COUNTRIES, GENRES } from '../filters'
import { FilterPanel } from '../components/FilterPanel'
import { MovieCard, MovieCardSkeleton } from '../components/MovieCard'
import { Pagination } from '../components/Pagination'
import '../catalog.css'

const PAGE_SIZE = 24

const nameOf = (list: { name: string; slug: string }[], slug: string) =>
  list.find((option) => option.slug === slug)?.name ?? slug

export function BrowsePage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const filters = parseBrowseFilters(params)
  const page = parsePage(params)
  const count = activeFilterCount(filters)
  const [panelOpen, setPanelOpen] = useState(() => !window.matchMedia('(max-width: 680px)').matches)

  const state = useAsync(
    (signal) => browseMovies(filters, { page: page - 1, size: PAGE_SIZE }, signal),
    JSON.stringify([filters, page]),
  )

  const go = (next: BrowseFilters) => navigate(browseHref(next))
  const search = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const q = new FormData(event.currentTarget).get('q')
    go({ ...filters, q: typeof q === 'string' ? q : undefined })
  }

  const removable: { label: string; remove: () => void }[] = []
  if (filters.type) {
    removable.push({
      label: filters.type === 'MOVIE' ? 'Phim lẻ' : 'Phim bộ',
      remove: () => go({ ...filters, type: undefined }),
    })
  }
  if (filters.genre) {
    removable.push({ label: nameOf(GENRES, filters.genre), remove: () => go({ ...filters, genre: undefined }) })
  }
  // One chip per country; removing one keeps the others.
  const countries = parseCountries(filters.country)
  for (const slug of countries) {
    removable.push({
      label: nameOf(COUNTRIES, slug),
      remove: () => go({ ...filters, country: countriesParam(toggleCountry(countries, slug)) }),
    })
  }

  return (
    <section className="browse page-width">
      <h1>{browseTitle(filters)}</h1>

      <div className="browse-tools">
        <form className="browse-search" role="search" onSubmit={search} key={filters.q ?? ''}>
          <Icon name="search" size={18} />
          <input name="q" type="search" defaultValue={filters.q ?? ''} placeholder="Bạn muốn xem phim gì hôm nay?" aria-label="Tìm phim" />
        </form>
        <button
          className={`filter-toggle ${panelOpen ? 'open' : ''}`}
          aria-expanded={panelOpen}
          onClick={() => setPanelOpen(!panelOpen)}
        >
          <Icon name="grid" size={16} /> Bộ lọc
          {count > 0 && <span className="filter-count">{count}</span>}
          <Icon name="chevronDown" size={14} className="filter-chevron" />
        </button>
      </div>

      {panelOpen && (
        <FilterPanel
          key={JSON.stringify([filters.type, filters.genre, filters.country])}
          applied={{ type: filters.type, genre: filters.genre, country: filters.country }}
          onApply={(draft) => go({ ...filters, ...draft })}
          onClear={() => go({ q: filters.q })}
        />
      )}

      {removable.length > 0 && (
        <div className="active-filters">
          <span>Đang lọc:</span>
          {removable.map((chip) => (
            <button key={chip.label} onClick={chip.remove} aria-label={`Bỏ lọc ${chip.label}`}>
              {chip.label} <Icon name="close" size={12} />
            </button>
          ))}
          <button className="clear-all" onClick={() => go({ q: filters.q })}>
            Xóa tất cả
          </button>
        </div>
      )}

      {state.status === 'error' ? (
        <div className="load-error" role="alert">
          <Icon name="alert" size={18} />
          <p>{errorMessage(state.error)}</p>
          <button onClick={state.reload}>
            Thử lại <Icon name="refresh" size={14} />
          </button>
        </div>
      ) : state.status === 'loading' ? (
        <div className="movie-grid">
          {Array.from({ length: 12 }, (_, i) => (
            <MovieCardSkeleton key={i} />
          ))}
        </div>
      ) : state.data.items.length === 0 ? (
        <div className="empty-state">
          <h2>Không tìm thấy phim phù hợp</h2>
          <p>Thử đổi từ khóa hoặc bỏ bớt bộ lọc.</p>
          <button className="btn secondary" onClick={() => go({})}>
            Xóa bộ lọc
          </button>
        </div>
      ) : (
        <>
          <div className="movie-grid">
            {state.data.items.map((movie) => (
              <MovieCard key={`${movie.providerKey}/${movie.movieId}`} movie={movie} />
            ))}
          </div>
          <Pagination
            current={page}
            hasNext={state.data.hasNext}
            hrefFor={(target) => browseHref(filters, target)}
          />
        </>
      )}
    </section>
  )
}
