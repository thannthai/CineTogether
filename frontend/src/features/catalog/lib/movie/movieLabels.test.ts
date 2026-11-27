import { describe, expect, it } from 'vitest'
import { differentTitle, languageLabel, progressLabel, youtubeId } from './movieLabels'

describe('progressLabel', () => {
  it('shows the latest episode and the total of a series that is still airing', () => {
    expect(progressLabel({ current: 8, total: 24, completed: false }, 'SERIES')).toBe('Tập 8/24')
    expect(progressLabel({ current: 8, total: null, completed: false }, 'SERIES')).toBe('Tập 8')
  })

  it('marks a finished series with the number of episodes', () => {
    expect(progressLabel({ current: 36, total: 36, completed: true }, 'SERIES')).toBe('Hoàn tất 36/36')
    expect(progressLabel({ current: 8, total: null, completed: true }, 'SERIES')).toBe('Hoàn tất 8/8')
    expect(progressLabel({ current: null, total: 12, completed: true }, 'SERIES')).toBe('Hoàn tất 12/12')
    expect(progressLabel({ current: null, total: null, completed: true }, 'SERIES')).toBe('Hoàn tất')
  })

  it('says Full for a single movie', () => {
    expect(progressLabel({ current: null, total: null, completed: true }, 'MOVIE')).toBe('Full')
  })

  it('shows nothing when there is nothing to say', () => {
    expect(progressLabel(undefined, 'SERIES')).toBeNull()
    expect(progressLabel(null, 'MOVIE')).toBeNull()
    expect(progressLabel({ current: null, total: null, completed: false }, 'SERIES')).toBeNull()
  })
})

describe('youtubeId', () => {
  const id = 'cya18nTNMzI'

  it('reads the common link shapes', () => {
    expect(youtubeId(`https://www.youtube.com/watch?v=${id}`)).toBe(id)
    expect(youtubeId(`https://youtube.com/watch?v=${id}&t=10s`)).toBe(id)
    expect(youtubeId(`https://youtu.be/${id}`)).toBe(id)
    expect(youtubeId(`https://www.youtube.com/embed/${id}`)).toBe(id)
    expect(youtubeId(`https://m.youtube.com/shorts/${id}`)).toBe(id)
  })

  it('gives null for anything that is not a YouTube video', () => {
    for (const bad of [null, undefined, '', 'not a link', 'https://vimeo.com/123', 'https://evil.example/watch?v=cya18nTNMzI']) {
      expect(youtubeId(bad)).toBeNull()
    }
  })

  it('only accepts a well-formed video id', () => {
    expect(youtubeId('https://www.youtube.com/watch?v=short')).toBeNull()
    expect(youtubeId('https://www.youtube.com/watch?v=abc"onload="x()')).toBeNull()
    expect(youtubeId('javascript:alert(1)')).toBeNull()
  })
})

describe('differentTitle', () => {
  it('is true when there is an original title that differs from the title', () => {
    expect(differentTitle({ title: 'Thứ Nữ', originalTitle: 'Noble Daughter' })).toBe(true)
  })

  it('is false without an original title or when it only differs by case or spaces', () => {
    expect(differentTitle({ title: 'Mai' })).toBe(false)
    expect(differentTitle({ title: 'Mai', originalTitle: null })).toBe(false)
    expect(differentTitle({ title: 'Mai', originalTitle: '  ' })).toBe(false)
    expect(differentTitle({ title: 'Oppenheimer', originalTitle: ' oppenheimer ' })).toBe(false)
  })
})

describe('languageLabel', () => {
  it('shows only Vietsub, whatever else the provider lists', () => {
    expect(languageLabel('Vietsub')).toBe('Vietsub')
    expect(languageLabel('Vietsub + Thuyết Minh')).toBe('Vietsub')
    expect(languageLabel('Thuyết Minh + Vietsub')).toBe('Vietsub')
    expect(languageLabel('Vietsub + Lồng Tiếng')).toBe('Vietsub')
  })

  it('has no tag without Vietsub', () => {
    expect(languageLabel('Thuyết Minh')).toBeNull()
    expect(languageLabel('Lồng Tiếng')).toBeNull()
    expect(languageLabel('')).toBeNull()
    expect(languageLabel(null)).toBeNull()
    expect(languageLabel(undefined)).toBeNull()
  })
})
