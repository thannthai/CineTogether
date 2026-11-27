export type TapZone = 'left' | 'middle' | 'right'
export type Tap = { zone: TapZone; at: number }

// The video is split in three vertical thirds, like on YouTube.
export function tapZone(x: number, width: number): TapZone {
  if (width <= 0) return 'middle'
  const share = x / width
  if (share < 1 / 3) return 'left'
  if (share < 2 / 3) return 'middle'
  return 'right'
}

// A tap that follows the previous one quickly on the same side. A third tap keeps counting, so a run of taps
// keeps skipping.
export function isRepeatTap(previous: Tap | null, zone: TapZone, now: number, windowMs: number): boolean {
  return previous !== null && previous.zone === zone && now - previous.at <= windowMs
}
