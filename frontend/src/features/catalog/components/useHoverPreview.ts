import { useEffect, useRef, useState } from 'react'

const OPEN_DELAY = 300
const CLOSE_DELAY = 120

export type Anchor = { left: number; top: number; width: number; height: number }

// Opens a preview after the pointer rests on a card for a moment. Touch screens have no hover:
// there a tap just opens the detail page.
export function useHoverPreview() {
  const [anchor, setAnchor] = useState<Anchor | null>(null)
  const timer = useRef(0)

  const open = (element: HTMLElement) => {
    window.clearTimeout(timer.current)
    if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return
    timer.current = window.setTimeout(() => {
      const { left, top, width, height } = element.getBoundingClientRect()
      setAnchor({ left, top, width, height })
    }, OPEN_DELAY)
  }
  const scheduleClose = () => {
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setAnchor(null), CLOSE_DELAY)
  }
  const keepOpen = () => window.clearTimeout(timer.current)
  const close = () => {
    window.clearTimeout(timer.current)
    setAnchor(null)
  }

  // The popup is positioned from a measurement, so it goes away when the page moves.
  useEffect(() => {
    if (!anchor) return
    window.addEventListener('scroll', close, { passive: true })
    window.addEventListener('resize', close)
    return () => {
      window.removeEventListener('scroll', close)
      window.removeEventListener('resize', close)
    }
  }, [anchor])
  useEffect(() => () => window.clearTimeout(timer.current), [])

  return { anchor, open, scheduleClose, keepOpen, close }
}
