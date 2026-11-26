import { useState } from 'react'
import { type BrowseFilters, type MovieTypeFilter } from '../browseFilters'
import { COUNTRIES, GENRES, type FilterOption } from '../filters'

type Draft = Pick<BrowseFilters, 'type' | 'genre' | 'country'>

const TYPES: { value: MovieTypeFilter | undefined; label: string }[] = [
  { value: undefined, label: 'Tất cả' },
  { value: 'MOVIE', label: 'Phim lẻ' },
  { value: 'SERIES', label: 'Phim bộ' },
]

type Props = {
  applied: Draft
  onApply: (draft: Draft) => void
  onClear: () => void
}

function Row<T extends string | undefined>({
  label,
  options,
  value,
  onChange,
}: {
  label: string
  options: { value: T; label: string }[]
  value: T
  onChange: (value: T) => void
}) {
  return (
    <div className="filter-row">
      <span className="filter-label">{label}</span>
      <div className="filter-chips">
        {options.map((option) => (
          <button
            key={option.label}
            className={option.value === value ? 'selected' : ''}
            aria-pressed={option.value === value}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  )
}

const slugOptions = (list: FilterOption[]) => [
  { value: undefined as string | undefined, label: 'Tất cả' },
  ...list.map((option) => ({ value: option.slug as string | undefined, label: option.name })),
]

// Choices are kept as a draft and only change the URL when "Áp dụng" is pressed.
// The page remounts this panel (by key) whenever the applied filters change.
export function FilterPanel({ applied, onApply, onClear }: Props) {
  const [draft, setDraft] = useState<Draft>(applied)
  const changed =
    draft.type !== applied.type || draft.genre !== applied.genre || draft.country !== applied.country

  return (
    <div className="filter-panel">
      <Row label="Loại phim" options={TYPES} value={draft.type} onChange={(type) => setDraft({ ...draft, type })} />
      <Row
        label="Thể loại"
        options={slugOptions(GENRES)}
        value={draft.genre}
        onChange={(genre) => setDraft({ ...draft, genre })}
      />
      <Row
        label="Quốc gia"
        options={slugOptions(COUNTRIES)}
        value={draft.country}
        onChange={(country) => setDraft({ ...draft, country })}
      />
      <div className="filter-actions">
        <button className="btn primary" disabled={!changed} onClick={() => onApply(draft)}>
          Áp dụng bộ lọc
        </button>
        <button className="btn secondary" onClick={onClear}>
          Xóa bộ lọc
        </button>
      </div>
    </div>
  )
}
