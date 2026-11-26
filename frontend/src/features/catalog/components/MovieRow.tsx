import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../../shared/lib/apiError'
import type { AsyncState } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import type { MovieSummary } from '../types'
import { MovieCard, MovieCardSkeleton } from './MovieCard'

type Props = {
  title: string
  seeAllTo: string
  state: AsyncState<MovieSummary[]>
  onRetry: () => void
}

// A titled, horizontally scrolling row of movies with its own loading and error states.
export function MovieRow({ title, seeAllTo, state, onRetry }: Props) {
  const scroller = useRef<HTMLDivElement>(null)
  const [edge, setEdge] = useState({ start: true, end: true })

  const measure = () => {
    const el = scroller.current
    if (!el) return
    setEdge({ start: el.scrollLeft <= 4, end: el.scrollLeft + el.clientWidth >= el.scrollWidth - 4 })
  }

  useEffect(() => {
    measure()
    window.addEventListener('resize', measure)
    return () => window.removeEventListener('resize', measure)
  }, [state.status])

  if (state.status === 'ready' && state.data.length === 0) return null

  const scrollBy = (direction: 1 | -1) => {
    const el = scroller.current
    if (el) el.scrollBy({ left: direction * el.clientWidth * 0.85, behavior: 'smooth' })
  }

  return (
    <section className="movie-row page-width">
      <div className="row-heading">
        <h2>{title}</h2>
        <div className="row-tools">
          <Link className="see-all" to={seeAllTo}>
            Xem tất cả <Icon name="arrow" size={14} />
          </Link>
          <button className="circle-btn" aria-label="Cuộn sang trái" disabled={edge.start} onClick={() => scrollBy(-1)}>
            <Icon name="chevronLeft" size={16} />
          </button>
          <button className="circle-btn" aria-label="Cuộn sang phải" disabled={edge.end} onClick={() => scrollBy(1)}>
            <Icon name="chevron" size={16} />
          </button>
        </div>
      </div>

      {state.status === 'error' ? (
        <div className="load-error" role="alert">
          <Icon name="alert" size={18} />
          <p>{errorMessage(state.error)}</p>
          <button onClick={onRetry}>
            Thử lại <Icon name="refresh" size={14} />
          </button>
        </div>
      ) : (
        <div className="row-scroller" ref={scroller} onScroll={measure}>
          {state.status === 'loading'
            ? Array.from({ length: 6 }, (_, i) => <MovieCardSkeleton key={i} />)
            : state.data.map((movie) => <MovieCard key={`${movie.providerKey}/${movie.movieId}`} movie={movie} />)}
        </div>
      )}
    </section>
  )
}
