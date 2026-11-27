import { describe, expect, it } from 'vitest'
import { filterRooms } from './roomFilter'
import type { MockRoom, RoomStatus } from './mockRooms'

const room = (id: string, status: RoomStatus, name = id, title = 'Phim', host = 'Lan'): MockRoom => ({
  id,
  name,
  status,
  movie: { providerKey: 'kkphim', movieId: id, title, type: 'MOVIE' },
  hostDisplayName: host,
  participantCount: 2,
  capacity: 6,
  available: status !== 'ended',
  episode: null,
})

const rooms = [room('a', 'live'), room('b', 'upcoming'), room('c', 'ended')]
const ids = (list: MockRoom[]) => list.map((r) => r.id)

describe('filterRooms', () => {
  it('shows every room, ended ones included, under "Tất cả"', () => {
    expect(ids(filterRooms(rooms, 'all', ''))).toEqual(['a', 'b', 'c'])
  })

  it('shows one status under the other tabs, so ended rooms only appear under "Tất cả"', () => {
    expect(ids(filterRooms(rooms, 'live', ''))).toEqual(['a'])
    expect(ids(filterRooms(rooms, 'upcoming', ''))).toEqual(['b'])
  })

  it('searches the room name, the movie title and the host, ignoring case and accents', () => {
    const list = [
      room('a', 'live', 'Hội mê phim khoa học viễn tưởng', 'Dune', 'Minh Anh'),
      room('b', 'live', 'Cày phim cuối tuần', 'Shōgun', 'Quang Huy'),
    ]
    expect(ids(filterRooms(list, 'all', 'VIEN TUONG'))).toEqual(['a'])
    expect(ids(filterRooms(list, 'all', 'shogun'))).toEqual(['b'])
    expect(ids(filterRooms(list, 'all', 'quang'))).toEqual(['b'])
    expect(ids(filterRooms(list, 'all', 'khong co'))).toEqual([])
  })

  it('combines the tab and the search, and ignores spaces around the search', () => {
    const list = [room('a', 'live', 'Cày phim'), room('b', 'upcoming', 'Cày phim')]
    expect(ids(filterRooms(list, 'upcoming', '  cay  '))).toEqual(['b'])
  })
})
