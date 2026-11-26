import { describe, expect, it } from 'vitest'
import { countriesParam, MAX_COUNTRIES, parseCountries, toggleCountry } from './countrySelection'

describe('parseCountries', () => {
  it('splits the comma-separated URL value', () => {
    expect(parseCountries('han-quoc,trung-quoc')).toEqual(['han-quoc', 'trung-quoc'])
  })

  it('gives an empty list when there is no value', () => {
    expect(parseCountries(undefined)).toEqual([])
    expect(parseCountries('')).toEqual([])
  })

  it('drops blanks and repeats and keeps the order', () => {
    expect(parseCountries('a,,b,a, ')).toEqual(['a', 'b'])
  })

  it('keeps only the first five, as a hand-edited address could have more', () => {
    expect(parseCountries('a,b,c,d,e,f,g')).toEqual(['a', 'b', 'c', 'd', 'e'])
  })
})

describe('toggleCountry', () => {
  it('adds a country that is not selected and removes one that is', () => {
    expect(toggleCountry(['a'], 'b')).toEqual(['a', 'b'])
    expect(toggleCountry(['a', 'b'], 'a')).toEqual(['b'])
  })

  it('does not add a country beyond the limit, but still removes one', () => {
    const full = ['a', 'b', 'c', 'd', 'e']
    expect(full).toHaveLength(MAX_COUNTRIES)
    expect(toggleCountry(full, 'f')).toEqual(full)
    expect(toggleCountry(full, 'c')).toEqual(['a', 'b', 'd', 'e'])
  })
})

describe('countriesParam', () => {
  it('joins the selection for the URL and gives nothing for "all"', () => {
    expect(countriesParam(['a', 'b'])).toBe('a,b')
    expect(countriesParam([])).toBeUndefined()
  })
})
