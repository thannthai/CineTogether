import { describe, expect, it } from 'vitest'
import { formatTime, seekTarget } from './playerTime'

describe('formatTime', () => {
  it('shows minutes and seconds under an hour', () => {
    expect(formatTime(0)).toBe('0:00')
    expect(formatTime(9)).toBe('0:09')
    expect(formatTime(125)).toBe('2:05')
    expect(formatTime(3599)).toBe('59:59')
  })

  it('adds hours from one hour on', () => {
    expect(formatTime(3600)).toBe('1:00:00')
    expect(formatTime(4487)).toBe('1:14:47')
  })

  it('drops the fraction instead of rounding up', () => {
    expect(formatTime(59.9)).toBe('0:59')
  })

  it('shows zero for a time that is not known yet', () => {
    for (const bad of [NaN, Infinity, -5]) expect(formatTime(bad)).toBe('0:00')
  })
})

describe('seekTarget', () => {
  it('moves by the step', () => {
    expect(seekTarget(30, 5, 600)).toBe(35)
    expect(seekTarget(30, -5, 600)).toBe(25)
  })

  it('never goes before the start or past the end', () => {
    expect(seekTarget(3, -5, 600)).toBe(0)
    expect(seekTarget(598, 5, 600)).toBe(600)
  })

  it('only stops at the start when the length is not known yet', () => {
    expect(seekTarget(30, 5, NaN)).toBe(35)
    expect(seekTarget(2, -5, Infinity)).toBe(0)
  })
})
