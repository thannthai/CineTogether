import { describe, expect, it } from 'vitest'
import { browseHref, parseBrowseFilters } from './browseFilters'
import { COUNTRIES, GENRES, MENU_COUNTRIES } from './filters'

describe('browseHref', () => {
  it('links to the plain browse page without filters', () => {
    expect(browseHref()).toBe('/movies')
    expect(browseHref({})).toBe('/movies')
  })

  it('uses the API parameter names', () => {
    expect(browseHref({ type: 'MOVIE', genre: 'hanh-dong', country: 'han-quoc' })).toBe(
      '/movies?type=MOVIE&genre=hanh-dong&country=han-quoc',
    )
  })

  it('omits blank values and trims the search text', () => {
    expect(browseHref({ genre: '', q: '   ' })).toBe('/movies')
    expect(browseHref({ q: '  dune  ' })).toBe('/movies?q=dune')
  })

  it('encodes search text with special characters', () => {
    expect(browseHref({ q: 'tom & jerry' })).toBe('/movies?q=tom+%26+jerry')
  })
})

describe('parseBrowseFilters', () => {
  it('round-trips what browseHref produces', () => {
    const filters = { type: 'SERIES', genre: 'tinh-cam', country: 'nhat-ban', q: 'đêm' } as const
    const query = browseHref(filters).split('?')[1]
    expect(parseBrowseFilters(new URLSearchParams(query))).toEqual(filters)
  })

  it('ignores an unknown type', () => {
    expect(parseBrowseFilters(new URLSearchParams('type=ANIME')).type).toBeUndefined()
  })
})

describe('filter option lists', () => {
  it('has unique slugs', () => {
    for (const list of [GENRES, COUNTRIES]) {
      const slugs = list.map((option) => option.slug)
      expect(new Set(slugs).size).toBe(slugs.length)
    }
  })

  it('resolves every header menu country', () => {
    expect(MENU_COUNTRIES).toHaveLength(12)
    expect(MENU_COUNTRIES.every((country) => country !== undefined)).toBe(true)
  })
})
