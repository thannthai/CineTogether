import { describe, expect, it } from 'vitest'
import { PREVIEW_HEIGHT, PREVIEW_WIDTH, previewPosition } from './previewPosition'

const viewport = { width: 1440, height: 900 }
const card = (left: number, top: number) => ({ left, top, width: 200, height: 300 })

describe('previewPosition', () => {
  it('centers the popup over the card', () => {
    const { left } = previewPosition(card(600, 300), viewport)
    expect(left).toBe(600 + 100 - PREVIEW_WIDTH / 2)
  })

  it('stays inside the left and right edges of the window', () => {
    expect(previewPosition(card(0, 300), viewport).left).toBe(12)
    expect(previewPosition(card(1400, 300), viewport).left).toBe(1440 - PREVIEW_WIDTH - 12)
  })

  it('starts a little above the card', () => {
    expect(previewPosition(card(600, 400), viewport).top).toBe(400 - 24)
  })

  it('never goes under the sticky header', () => {
    expect(previewPosition(card(600, 10), viewport).top).toBe(92)
  })

  it('never runs past the bottom of the window', () => {
    expect(previewPosition(card(600, 850), viewport).top).toBe(900 - PREVIEW_HEIGHT - 12)
  })
})
