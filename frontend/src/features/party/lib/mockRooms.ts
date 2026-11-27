import type { MovieSummary } from '../../catalog'

// Sample public rooms, shaped like a PublicRoomSummary of docs/design/api.md §2.4 plus the movie it plays. Rooms do not
// exist yet: the page shows these as an illustration and says so.
export type RoomStatus = 'live' | 'upcoming' | 'ended'

export type MockRoom = {
  id: string
  name: string
  status: RoomStatus
  movie: MovieSummary
  hostDisplayName: string
  participantCount: number
  capacity: number
  available: boolean
  // The episode being watched, for a series.
  episode: number | null
}

const SAMPLES: { name: string; host: string; count: number; capacity: number; status: RoomStatus; episode: number }[] = [
  { name: 'Hội mê phim khoa học viễn tưởng', host: 'Minh Anh', count: 4, capacity: 6, status: 'live', episode: 3 },
  { name: 'Tối nay xem phim gì?', host: 'Phương Linh', count: 2, capacity: 8, status: 'upcoming', episode: 1 },
  { name: 'Cày phim cuối tuần', host: 'Quang Huy', count: 3, capacity: 6, status: 'live', episode: 5 },
  { name: 'Góc phim kinh dị', host: 'Hải Đăng', count: 6, capacity: 6, status: 'live', episode: 2 },
  { name: 'Hội yêu phim hoạt hình', host: 'Mộc Miên', count: 4, capacity: 8, status: 'ended', episode: 8 },
  { name: 'Xem phim cùng người mới', host: 'Hoàng Nam', count: 3, capacity: 6, status: 'upcoming', episode: 1 },
]

export function mockRooms(movies: MovieSummary[]): MockRoom[] {
  return movies.slice(0, SAMPLES.length).map((movie, index) => {
    const { name, host, count, capacity, status, episode } = SAMPLES[index]
    return {
      id: `sample-${index}`,
      name,
      status,
      movie,
      hostDisplayName: host,
      participantCount: count,
      capacity,
      available: status !== 'ended' && count < capacity,
      episode: movie.type === 'SERIES' ? episode : null,
    }
  })
}
