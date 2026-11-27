import { describe, expect, it } from 'vitest'
import { DEFAULT_PREFS, parsePrefs, SPEEDS } from './playerPrefs'

describe('parsePrefs', () => {
  it('reads saved preferences', () => {
    expect(parsePrefs('{"volume":0.4,"muted":true,"rate":1.5}')).toEqual({ volume: 0.4, muted: true, rate: 1.5 })
  })

  it('uses the defaults when nothing is saved or the text is broken', () => {
    expect(parsePrefs(null)).toEqual(DEFAULT_PREFS)
    expect(parsePrefs('not json')).toEqual(DEFAULT_PREFS)
    expect(parsePrefs('42')).toEqual(DEFAULT_PREFS)
  })

  it('fixes each field on its own', () => {
    expect(parsePrefs('{"volume":7,"muted":"yes","rate":3}')).toEqual(DEFAULT_PREFS)
    expect(parsePrefs('{"volume":0.5}')).toEqual({ ...DEFAULT_PREFS, volume: 0.5 })
  })

  it('only accepts a speed the menu offers', () => {
    expect(SPEEDS).toContain(1.25)
    expect(parsePrefs('{"rate":1.1}').rate).toBe(1)
  })
})
