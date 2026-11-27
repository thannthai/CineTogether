import type { MockRoom } from './mockRooms'

// "2024 · Tập 3": the year and, for a series, the episode being watched.
export function movieLine(room: MockRoom): string {
  return [room.movie.releaseYear, room.episode ? `Tập ${room.episode}` : null].filter(Boolean).join(' · ')
}

// What the foot of a room card says about joining.
export function joinNote(room: MockRoom): string {
  if (room.status === 'ended') return 'Phòng đã kết thúc'
  return room.available ? 'Còn chỗ tham gia' : 'Phòng đã đầy'
}
