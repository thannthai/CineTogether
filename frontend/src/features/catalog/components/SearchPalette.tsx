import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { errorMessage } from '../../../shared/lib/apiError'
import { useAsync } from '../../../shared/lib/useAsync'
import { Icon } from '../../../shared/ui/Icon'
import { browseMovies } from '../api/catalogApi'
import { browseHref } from '../lib/browse/browseFilters'
import { differentTitle, progressLabel } from '../lib/movie/movieLabels'
import { moviePath } from '../lib/movie/moviePaths'
import type { MoviePage } from '../api/types'

const MIN_LENGTH = 2
const MAX_SUGGESTIONS = 6
const EMPTY: MoviePage = { items: [], page: 0, size: 0, hasNext: false }

// Quick search opened from the header (button or Cmd/Ctrl+K): up to six suggestions as you type.
export function SearchPalette({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate()
  const dialogRef = useRef<HTMLDialogElement>(null)
  const [query, setQuery] = useState('')
  const [term, setTerm] = useState('')
  const [active, setActive] = useState(-1)

  useEffect(() => {
    dialogRef.current?.showModal()
  }, [])

  // Wait for a pause in typing before asking the provider.
  useEffect(() => {
    const timer = window.setTimeout(() => setTerm(query.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [query])

  const state = useAsync(
    (signal) =>
      term.length < MIN_LENGTH
        ? Promise.resolve(EMPTY)
        : browseMovies({ q: term }, { size: MAX_SUGGESTIONS }, signal),
    `suggest:${term}`,
  )
  const items = state.status === 'ready' ? state.data.items : []
  const typed = query.trim()
  const settled = typed === term && typed.length >= MIN_LENGTH

  const openMovie = (index: number) => {
    onClose()
    navigate(moviePath(items[index]))
  }
  const seeAll = () => {
    onClose()
    navigate(browseHref({ q: typed }))
  }

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((i) => Math.min(i + 1, items.length - 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((i) => Math.max(i - 1, -1))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      if (active >= 0 && items[active]) openMovie(active)
      else if (typed) seeAll()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="palette"
      aria-label="Tìm kiếm phim"
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="palette-field">
        <Icon name="search" size={20} />
        <input
          autoFocus
          value={query}
          placeholder="Tìm phim bạn muốn xem..."
          aria-label="Từ khóa"
          onChange={(event) => {
            setQuery(event.target.value)
            setActive(-1)
          }}
          onKeyDown={onKeyDown}
        />
        <button className="icon-btn" aria-label="Đóng" onClick={onClose}>
          <Icon name="close" size={18} />
        </button>
      </div>

      <div className="palette-body">
        {typed.length < MIN_LENGTH && <p className="palette-hint">Nhập ít nhất {MIN_LENGTH} ký tự để tìm phim.</p>}
        {settled && state.status === 'loading' && (
          <div className="palette-loading" aria-label="Đang tìm">
            {Array.from({ length: 3 }, (_, i) => (
              <span key={i} className="skeleton" />
            ))}
          </div>
        )}
        {settled && state.status === 'error' && <p className="palette-hint">{errorMessage(state.error)}</p>}
        {settled && state.status === 'ready' && items.length === 0 && (
          <p className="palette-hint">Không tìm thấy kết quả cho “{typed}”.</p>
        )}
        {settled && items.length > 0 && (
          <ul role="listbox" aria-label="Gợi ý phim">
            {items.map((movie, index) => (
              <li key={`${movie.providerKey}/${movie.movieId}`} role="option" aria-selected={index === active}>
                <button
                  className={`palette-row ${index === active ? 'active' : ''}`}
                  onMouseEnter={() => setActive(index)}
                  onClick={() => openMovie(index)}
                >
                  {movie.posterUrl ? <img src={movie.posterUrl} alt="" /> : <span className="palette-noposter" />}
                  <span className="palette-copy">
                    <strong>{movie.title}</strong>
                    {differentTitle(movie) && <small>{movie.originalTitle}</small>}
                    <small>{[movie.releaseYear, progressLabel(movie.progress, movie.type)].filter(Boolean).join(' • ')}</small>
                  </span>
                  <Icon name="chevron" size={15} />
                </button>
              </li>
            ))}
          </ul>
        )}
        {typed.length >= MIN_LENGTH && (
          <button className="palette-all" onClick={seeAll}>
            Xem tất cả kết quả cho “{typed}” <Icon name="arrow" size={14} />
          </button>
        )}
      </div>
      <p className="palette-foot">
        <kbd>↑</kbd> <kbd>↓</kbd> để chọn · <kbd>Enter</kbd> để mở · <kbd>Esc</kbd> để đóng
      </p>
    </dialog>
  )
}
