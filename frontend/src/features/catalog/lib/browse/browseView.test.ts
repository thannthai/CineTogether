import { describe, expect, it } from 'vitest'
import { browseHref, parsePage } from './browseFilters'
import { activeFilterCount, browseTitle, pageWindow } from './browseView'

describe('page in the URL', () => {
  it('reads a human 1-based page and falls back to 1', () => {
    expect(parsePage(new URLSearchParams('page=3'))).toBe(3)
    for (const bad of ['', 'page=0', 'page=-2', 'page=abc', 'page=2.5']) {
      expect(parsePage(new URLSearchParams(bad))).toBe(1)
    }
  })

  it('writes the page only when it is not the first', () => {
    expect(browseHref({ genre: 'hanh-dong' }, 1)).toBe('/movies?genre=hanh-dong')
    expect(browseHref({ genre: 'hanh-dong' }, 3)).toBe('/movies?genre=hanh-dong&page=3')
    expect(browseHref({}, 2)).toBe('/movies?page=2')
  })
})

describe('browseTitle', () => {
  it('names the default and the type pages', () => {
    expect(browseTitle({})).toBe('Phim mới cập nhật')
    expect(browseTitle({ type: 'MOVIE' })).toBe('Phim lẻ')
    expect(browseTitle({ type: 'SERIES' })).toBe('Phim bộ')
  })

  it('uses the readable names of a genre or a country', () => {
    expect(browseTitle({ genre: 'hanh-dong' })).toBe('Hành Động')
    expect(browseTitle({ country: 'han-quoc' })).toBe('Hàn Quốc')
  })

  it('joins combined filters and puts a search first', () => {
    expect(browseTitle({ type: 'MOVIE', genre: 'hanh-dong' })).toBe('Phim lẻ · Hành Động')
    expect(browseTitle({ q: 'dune', genre: 'hanh-dong' })).toBe('Kết quả cho “dune”')
  })

  it('shows an unknown slug as it is', () => {
    expect(browseTitle({ genre: 'khong-co' })).toBe('khong-co')
  })
})

describe('activeFilterCount', () => {
  it('counts type, genre and country but not the search text', () => {
    expect(activeFilterCount({})).toBe(0)
    expect(activeFilterCount({ q: 'dune' })).toBe(0)
    expect(activeFilterCount({ type: 'MOVIE', genre: 'x', country: 'y' })).toBe(3)
  })
})

// The API only tells whether a next page exists, never how many pages there are.
describe('pageWindow', () => {
  it('shows only page 1 and no steps when there is nothing else', () => {
    expect(pageWindow(1, false)).toEqual({ items: [1], prev: null, next: null })
  })

  it('offers the next page when there is one', () => {
    expect(pageWindow(1, true)).toEqual({ items: [1, 2], prev: null, next: 2 })
  })

  it('shows up to two earlier pages and the next one', () => {
    expect(pageWindow(4, true)).toEqual({ items: [1, 2, 3, 4, 5], prev: 3, next: 5 })
  })

  it('keeps the first page and an ellipsis once far from the start', () => {
    expect(pageWindow(9, false)).toEqual({ items: [1, '…', 7, 8, 9], prev: 8, next: null })
  })

  it('does not add an ellipsis when it would hide nothing', () => {
    expect(pageWindow(3, true)).toEqual({ items: [1, 2, 3, 4], prev: 2, next: 4 })
  })
})

describe('several countries', () => {
  it('lists their readable names in the title', () => {
    expect(browseTitle({ country: 'han-quoc,trung-quoc' })).toBe('Hàn Quốc, Trung Quốc')
    expect(browseTitle({ type: 'SERIES', country: 'han-quoc,khong-co' })).toBe('Phim bộ · Hàn Quốc, khong-co')
  })

  it('counts each country as a filter, like the chips that are shown', () => {
    expect(activeFilterCount({ type: 'SERIES', country: 'han-quoc,trung-quoc' })).toBe(3)
  })
})

describe('the cinema list', () => {
  it('names the page and combines with the other filters', () => {
    expect(browseTitle({ list: 'CINEMA' })).toBe('Phim chiếu rạp')
    expect(browseTitle({ list: 'CINEMA', country: 'han-quoc' })).toBe('Phim chiếu rạp · Hàn Quốc')
  })

  it('names the anime list too', () => {
    expect(browseTitle({ list: 'ANIME', country: 'nhat-ban' })).toBe('Anime · Nhật Bản')
  })

  it('wins over the type, as it does in the API', () => {
    expect(browseTitle({ list: 'CINEMA', type: 'SERIES' })).toBe('Phim chiếu rạp')
    expect(activeFilterCount({ list: 'CINEMA', type: 'SERIES', genre: 'hanh-dong' })).toBe(2)
  })
})

describe('the year', () => {
  it('ends the title and counts as a filter', () => {
    expect(browseTitle({ year: 2026 })).toBe('2026')
    expect(browseTitle({ type: 'SERIES', country: 'han-quoc', year: 2025 })).toBe('Phim bộ · Hàn Quốc · 2025')
    expect(activeFilterCount({ type: 'SERIES', year: 2026 })).toBe(2)
  })
})
