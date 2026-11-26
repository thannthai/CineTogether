import { describe, expect, it } from 'vitest'
import { neighbours, pickEpisode } from './pickEpisode'

const episodes = [
  { episodeId: 'tap-01', title: 'Tập 01' },
  { episodeId: 'tap-02', title: 'Tập 02' },
  { episodeId: 'tap-03', title: 'Tập 03' },
]

describe('pickEpisode', () => {
  it('finds the episode asked for in the URL', () => {
    expect(pickEpisode(episodes, 'tap-02')).toEqual({ episode: episodes[1], index: 1 })
  })

  it('falls back to the first episode when none or an unknown one is asked for', () => {
    expect(pickEpisode(episodes, null)).toEqual({ episode: episodes[0], index: 0 })
    expect(pickEpisode(episodes, 'tap-99')).toEqual({ episode: episodes[0], index: 0 })
  })

  it('gives nothing for a movie without episodes', () => {
    expect(pickEpisode([], 'tap-01')).toBeNull()
  })
})

describe('neighbours', () => {
  it('gives the episodes before and after', () => {
    expect(neighbours(episodes, 1)).toEqual({ previous: episodes[0], next: episodes[2] })
  })

  it('has no previous on the first episode and no next on the last', () => {
    expect(neighbours(episodes, 0)).toEqual({ previous: null, next: episodes[1] })
    expect(neighbours(episodes, 2)).toEqual({ previous: episodes[1], next: null })
  })

  it('has none for a single episode', () => {
    expect(neighbours([episodes[0]], 0)).toEqual({ previous: null, next: null })
  })
})
