export const PREVIEW_WIDTH = 360
// Used to keep the popup on screen; the real height depends on the synopsis.
export const PREVIEW_HEIGHT = 400
const EDGE = 12
// The sticky header is 80px tall.
const HEADER = 92

type Box = { left: number; top: number; width: number; height: number }

// Fixed-position coordinates of the hover popup for a card, clamped to the window.
export function previewPosition(card: Box, viewport: { width: number; height: number }) {
  const left = Math.min(
    Math.max(card.left + card.width / 2 - PREVIEW_WIDTH / 2, EDGE),
    viewport.width - PREVIEW_WIDTH - EDGE,
  )
  const top = Math.max(
    Math.min(card.top - 24, viewport.height - PREVIEW_HEIGHT - EDGE),
    HEADER,
  )
  return { left, top }
}
