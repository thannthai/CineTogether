import { describe, expect, it } from 'vitest'
import { moviePath, typeLabel, watchPath } from './moviePaths'

describe('moviePath', () => {
  it('builds the detail URL from the provider identifiers', () => {
    expect(moviePath({ providerKey: 'kkphim', movieId: 'dune-2' })).toBe('/movies/kkphim/dune-2')
  })

  it('encodes identifiers so they stay one path segment', () => {
    expect(moviePath({ providerKey: 'kk', movieId: 'a/b c' })).toBe('/movies/kk/a%2Fb%20c')
  })
})

describe('watchPath', () => {
  it('opens the watch page, optionally on one episode', () => {
    const movie = { providerKey: 'kkphim', movieId: 'dune-2' }
    expect(watchPath(movie)).toBe('/movies/kkphim/dune-2/watch')
    expect(watchPath(movie, 'tap-03')).toBe('/movies/kkphim/dune-2/watch?episode=tap-03')
  })
})

describe('typeLabel', () => {
  it('uses the Vietnamese names', () => {
    expect(typeLabel('MOVIE')).toBe('Phim lẻ')
    expect(typeLabel('SERIES')).toBe('Phim bộ')
  })
})
