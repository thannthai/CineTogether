import { describe, expect, it } from 'vitest'
import { isRepeatTap, tapZone } from './tapGesture'

describe('tapZone', () => {
  it('splits the video into a left, a middle and a right third', () => {
    expect(tapZone(0, 900)).toBe('left')
    expect(tapZone(299, 900)).toBe('left')
    expect(tapZone(300, 900)).toBe('middle')
    expect(tapZone(599, 900)).toBe('middle')
    expect(tapZone(600, 900)).toBe('right')
    expect(tapZone(900, 900)).toBe('right')
  })

  it('treats a tap outside the box as the nearest side and an empty box as the middle', () => {
    expect(tapZone(-5, 900)).toBe('left')
    expect(tapZone(950, 900)).toBe('right')
    expect(tapZone(10, 0)).toBe('middle')
  })
})

describe('isRepeatTap', () => {
  const first = { zone: 'right', at: 1000 } as const

  it('is not a repeat for the very first tap', () => {
    expect(isRepeatTap(null, 'right', 1000, 280)).toBe(false)
  })

  it('is a repeat when the same side is tapped again quickly', () => {
    expect(isRepeatTap(first, 'right', 1200, 280)).toBe(true)
    expect(isRepeatTap(first, 'right', 1280, 280)).toBe(true)
  })

  it('is not a repeat when the second tap comes too late', () => {
    expect(isRepeatTap(first, 'right', 1281, 280)).toBe(false)
  })

  it('is not a repeat when the other side is tapped', () => {
    expect(isRepeatTap(first, 'left', 1100, 280)).toBe(false)
    expect(isRepeatTap(first, 'middle', 1100, 280)).toBe(false)
  })
})
