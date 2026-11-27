import { describe, expect, it } from 'vitest'
import { fullscreenMethod } from './fullscreenSupport'

const fn = () => {}

describe('fullscreenMethod', () => {
  it('puts the whole player on screen when the browser can do that, so our controls stay', () => {
    expect(fullscreenMethod({ requestFullscreen: fn }, { webkitEnterFullscreen: fn })).toBe('container')
  })

  it('falls back to the video itself on iPhone Safari, which cannot do it for other elements', () => {
    expect(fullscreenMethod({}, { webkitEnterFullscreen: fn })).toBe('video')
  })

  it('reports that full screen is not available at all', () => {
    expect(fullscreenMethod({}, {})).toBe('none')
  })

  it('ignores a property that is not a function', () => {
    expect(fullscreenMethod({ requestFullscreen: true }, { webkitEnterFullscreen: fn })).toBe('video')
  })
})
