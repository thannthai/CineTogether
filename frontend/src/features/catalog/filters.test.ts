import { describe, expect, it } from 'vitest'
import { genreSlug, yearOptions } from './filters'

describe('genreSlug', () => {
  it('finds the slug of a genre by its name', () => {
    expect(genreSlug('Hành Động')).toBe('hanh-dong')
    expect(genreSlug('Chính Kịch')).toBe('chinh-kich')
  })

  it('ignores letter case and surrounding spaces', () => {
    expect(genreSlug('  hành động ')).toBe('hanh-dong')
  })

  it('gives undefined for a genre the filter does not know', () => {
    expect(genreSlug('Hoạt Hình')).toBeUndefined()
    expect(genreSlug('')).toBeUndefined()
  })
})

describe('yearOptions', () => {
  it('lists the current year and the earlier ones, newest first', () => {
    expect(yearOptions(2026, 4)).toEqual([2026, 2025, 2024, 2023])
    expect(yearOptions(2027, 2)).toEqual([2027, 2026])
  })
})
