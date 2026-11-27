import { describe, expect, it } from 'vitest'
import { fillFromPreviousYear } from './recentYears'
import type { MovieSummary } from '../../api/types'

const movie = (movieId: string): MovieSummary => ({ providerKey: 'kkphim', movieId, title: movieId, type: 'MOVIE' })
const ids = (movies: MovieSummary[]) => movies.map((m) => m.movieId)

describe('fillFromPreviousYear', () => {
  it('keeps this year first and fills the missing places with last year', () => {
    expect(ids(fillFromPreviousYear([movie('a')], [movie('x'), movie('y')], 3))).toEqual(['a', 'x', 'y'])
  })

  it('does not use last year when this year already fills every place', () => {
    expect(ids(fillFromPreviousYear([movie('a'), movie('b')], [movie('x')], 2))).toEqual(['a', 'b'])
  })

  it('never repeats a movie and never goes over the size', () => {
    expect(ids(fillFromPreviousYear([movie('a')], [movie('a'), movie('x'), movie('y')], 2))).toEqual(['a', 'x'])
  })
})
