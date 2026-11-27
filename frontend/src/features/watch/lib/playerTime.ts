const pad = (n: number) => String(n).padStart(2, '0')

// 125 -> "2:05", 4487 -> "1:14:47". A time that is not known yet (NaN, Infinity) shows as 0:00.
export function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '0:00'
  const total = Math.floor(seconds)
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const rest = total % 60
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${minutes}:${pad(rest)}`
}

// Where a seek of `delta` seconds lands, kept between the start and the end of the video.
export function seekTarget(current: number, delta: number, duration: number): number {
  const target = Math.max(0, current + delta)
  return Number.isFinite(duration) ? Math.min(target, duration) : target
}
