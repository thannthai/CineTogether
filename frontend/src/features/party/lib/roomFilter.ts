import type { MockRoom } from './mockRooms'

export type RoomTab = 'all' | 'live' | 'upcoming'

const plain = (text: string) => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLowerCase()

// "Tất cả" shows every room, including the ended ones; the other tabs show one status. The search looks at the room
// name, the movie title and the host, ignoring case and accents.
export function filterRooms(rooms: MockRoom[], tab: RoomTab, query: string): MockRoom[] {
  const needle = plain(query.trim())
  return rooms.filter((room) => {
    if (tab !== 'all' && room.status !== tab) return false
    if (!needle) return true
    return [room.name, room.movie.title, room.hostDisplayName].some((text) => plain(text).includes(needle))
  })
}
