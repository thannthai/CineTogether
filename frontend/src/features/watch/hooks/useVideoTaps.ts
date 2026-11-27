import { useEffect, useRef, useState, type MouseEvent, type PointerEvent } from 'react'
import { isRepeatTap, tapZone, type Tap } from '../lib/tapGesture'

const TAP_WINDOW_MS = 280
const FLASH_MS = 650

export type SkipFlash = { side: 'left' | 'right'; id: number }

type Actions = {
  togglePlay: () => void
  // -1 for back, 1 for ahead.
  skip: (direction: -1 | 1) => void
  toggleFullscreen: () => void
}

// Mouse: a click plays or pauses, a double click goes full screen. Touch: one tap plays or pauses (after a short
// wait, to see whether a second tap follows) and two taps on the left or right third skip back or ahead.
export function useVideoTaps({ togglePlay, skip, toggleFullscreen }: Actions) {
  const pointer = useRef('mouse')
  const lastTap = useRef<Tap | null>(null)
  const pending = useRef(0)
  const flashTimer = useRef(0)
  const flashCount = useRef(0)
  const [flash, setFlash] = useState<SkipFlash | null>(null)

  useEffect(
    () => () => {
      window.clearTimeout(pending.current)
      window.clearTimeout(flashTimer.current)
    },
    [],
  )

  const showFlash = (side: SkipFlash['side']) => {
    window.clearTimeout(flashTimer.current)
    flashCount.current += 1
    setFlash({ side, id: flashCount.current })
    flashTimer.current = window.setTimeout(() => setFlash(null), FLASH_MS)
  }

  const onPointerDown = (event: PointerEvent<HTMLVideoElement>) => {
    pointer.current = event.pointerType
  }

  const onClick = (event: MouseEvent<HTMLVideoElement>) => {
    if (pointer.current !== 'touch') {
      togglePlay()
      return
    }
    const box = event.currentTarget.getBoundingClientRect()
    const zone = tapZone(event.clientX - box.left, box.width)
    const now = performance.now()
    const repeat = isRepeatTap(lastTap.current, zone, now, TAP_WINDOW_MS)
    lastTap.current = { zone, at: now }

    window.clearTimeout(pending.current)
    if (!repeat) {
      pending.current = window.setTimeout(togglePlay, TAP_WINDOW_MS)
    } else if (zone !== 'middle') {
      skip(zone === 'left' ? -1 : 1)
      showFlash(zone)
    }
  }

  // On a touch screen the double tap is handled above; only the mouse goes full screen this way.
  const onDoubleClick = () => {
    if (pointer.current !== 'touch') toggleFullscreen()
  }

  return { flash, handlers: { onPointerDown, onClick, onDoubleClick } }
}
