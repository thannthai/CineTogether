import { describe, expect, it } from 'vitest'
import { pickDistinct } from './coverPicks'
import type { MovieSummary } from './types'

const movie = (id: string, year: number | null, poster: string | null = `${id}.jpg`): MovieSummary => ({
  providerKey: 'kkphim',
  movieId: id,
  title: id,
  type: 'MOVIE',
  posterUrl: poster,
  releaseYear: year,
  genres: [],
})
const ids = (picks: (MovieSummary | null)[]) => picks.map((pick) => pick?.movieId ?? null)

describe('pickDistinct', () => {
  it('gives each theme the first movie of its own list', () => {
    const picks = pickDistinct([[movie('a', 2026), movie('b', 2026)], [movie('c', 2026)]], 2026)
    expect(ids(picks)).toEqual(['a', 'c'])
  })

  it('never uses the same movie for two themes', () => {
    const shared = movie('shared', 2026)
    const picks = pickDistinct([[shared, movie('a', 2026)], [shared, movie('b', 2026)]], 2026)
    expect(ids(picks)).toEqual(['shared', 'b'])
  })

  it('prefers a movie released this year over an older one', () => {
    const picks = pickDistinct([[movie('old', 2019), movie('new', 2026)]], 2026)
    expect(ids(picks)).toEqual(['new'])
  })

  it('falls back to an older movie when nothing is new', () => {
    expect(ids(pickDistinct([[movie('old', 2019)]], 2026))).toEqual(['old'])
  })

  it('skips a movie without a poster', () => {
    const picks = pickDistinct([[movie('bare', 2026, null), movie('shown', 2026)]], 2026)
    expect(ids(picks)).toEqual(['shown'])
  })

  it('gives null for a theme with nothing usable', () => {
    expect(ids(pickDistinct([[], [movie('a', 2026, null)]], 2026))).toEqual([null, null])
  })

  it('lets an earlier theme pick first, so a later one takes its next choice', () => {
    const picks = pickDistinct([[movie('a', 2026)], [movie('a', 2026), movie('b', 2026)], [movie('a', 2026)]], 2026)
    expect(ids(picks)).toEqual(['a', 'b', null])
  })
})
