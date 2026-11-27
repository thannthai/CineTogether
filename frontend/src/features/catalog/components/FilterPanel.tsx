import { useState } from 'react'
import { type BrowseFilters, type MovieTypeFilter } from '../browseFilters'
import { countriesParam, MAX_COUNTRIES, parseCountries, toggleCountry } from '../countrySelection'
import { COUNTRIES, GENRES, yearOptions, type FilterOption } from '../filters'

type Draft = Pick<BrowseFilters, 'type' | 'genre' | 'country' | 'year'>

const TYPES: { value: MovieTypeFilter | undefined; label: string }[] = [
  { value: undefined, label: 'Tất cả' },
  { value: 'MOVIE', label: 'Phim lẻ' },
  { value: 'SERIES', label: 'Phim bộ' },
]

type Props = {
  applied: Draft
  onApply: (draft: Draft) => void
  onClear: () => void
  // The cinema list ignores the type, so its row is not offered.
  hideType?: boolean
}

function Row<T extends string | number | undefined>({
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

// Several choices at once. "Tất cả" is lit only while nothing else is chosen, and choosing it clears the others.
function MultiRow({
  label,
  options,
  selected,
  onChange,
}: {
  label: string
  options: FilterOption[]
  selected: string[]
  onChange: (selected: string[]) => void
}) {
  const full = selected.length >= MAX_COUNTRIES
  return (
    <div className="filter-row">
      <span className="filter-label">{label}</span>
      <div className="filter-chips">
        <button
          className={selected.length === 0 ? 'selected' : ''}
          aria-pressed={selected.length === 0}
          onClick={() => onChange([])}
        >
          Tất cả
        </button>
        {options.map((option) => {
          const on = selected.includes(option.slug)
          return (
            <button
              key={option.slug}
              className={on ? 'selected' : ''}
              aria-pressed={on}
              disabled={full && !on}
              title={full && !on ? `Chọn tối đa ${MAX_COUNTRIES} quốc gia` : undefined}
              onClick={() => onChange(toggleCountry(selected, option.slug))}
            >
              {option.name}
            </button>
          )
        })}
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
export function FilterPanel({ applied, onApply, onClear, hideType }: Props) {
  const [draft, setDraft] = useState<Draft>({ ...applied, country: countriesParam(parseCountries(applied.country)) })
  // Read once when the panel opens, so the list of years does not change while it is open.
  const [thisYear] = useState(() => new Date().getFullYear())
  const changed =
    draft.type !== applied.type ||
    draft.genre !== applied.genre ||
    draft.country !== countriesParam(parseCountries(applied.country)) ||
    draft.year !== applied.year

  return (
    <div className="filter-panel">
      {!hideType && (
        <Row label="Loại phim" options={TYPES} value={draft.type} onChange={(type) => setDraft({ ...draft, type })} />
      )}
      <Row
        label="Thể loại"
        options={slugOptions(GENRES)}
        value={draft.genre}
        onChange={(genre) => setDraft({ ...draft, genre })}
      />
      <MultiRow
        label="Quốc gia"
        options={COUNTRIES}
        selected={parseCountries(draft.country)}
        onChange={(selected) => setDraft({ ...draft, country: countriesParam(selected) })}
      />
      <Row
        label="Năm"
        options={[
          { value: undefined as number | undefined, label: 'Tất cả' },
          ...yearOptions(thisYear).map((year) => ({ value: year as number | undefined, label: String(year) })),
        ]}
        value={draft.year}
        onChange={(year) => setDraft({ ...draft, year })}
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
