import { describe, expect, it } from 'vitest'
import { activeNav } from './activeNav'

const nav = (url: string) => {
  const [pathname, query = ''] = url.split('?')
  return activeNav(pathname, new URLSearchParams(query))
}

describe('activeNav', () => {
  it('highlights nothing on the home page or unrelated pages', () => {
    expect(nav('/')).toBeNull()
    expect(nav('/movies/provider/123')).toBeNull()
    expect(nav('/whatever')).toBeNull()
  })

  it('highlights "new" on the unfiltered browse page', () => {
    expect(nav('/movies')).toBe('new')
  })

  it('highlights the movie and series types', () => {
    expect(nav('/movies?type=MOVIE')).toBe('movie')
    expect(nav('/movies?type=SERIES')).toBe('series')
  })

  it('highlights the cinema list, which wins over the type like in the API', () => {
    expect(nav('/movies?list=CINEMA')).toBe('cinema')
    expect(nav('/movies?list=CINEMA&type=SERIES&genre=hanh-dong')).toBe('cinema')
  })

  it('highlights the anime list', () => {
    expect(nav('/movies?list=ANIME')).toBe('anime')
    expect(nav('/movies?list=ANIME&country=nhat-ban')).toBe('anime')
  })

  it('highlights a genre or a country filter used alone', () => {
    expect(nav('/movies?genre=hanh-dong')).toBe('genre')
    expect(nav('/movies?country=han-quoc')).toBe('country')
  })

  it('highlights nothing when filters are combined or a search is active', () => {
    expect(nav('/movies?genre=hanh-dong&country=han-quoc')).toBeNull()
    expect(nav('/movies?q=dune')).toBeNull()
    expect(nav('/movies?type=MOVIE&genre=hanh-dong')).toBe('movie')
  })

  it('highlights the watch-together page', () => {
    expect(nav('/together')).toBe('together')
  })
})
