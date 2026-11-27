import type { MovieSummary } from '../../catalog'

// Sample public rooms, shaped like a PublicRoomSummary of docs/design/api.md §2.4 plus the movie it plays. Rooms do not
// exist yet: the page shows these as an illustration and says so.
export type MockRoom = {
  id: string
  movie: MovieSummary
  hostDisplayName: string
  participantCount: number
  capacity: number
  available: boolean
}

const SAMPLES = [
  { host: 'Minh', count: 4, capacity: 6 },
  { host: 'Lan', count: 2, capacity: 8 },
  { host: 'Hoàng', count: 3, capacity: 6 },
  { host: 'Thảo', count: 6, capacity: 6 },
  { host: 'Quang', count: 1, capacity: 4 },
  { host: 'Mai', count: 5, capacity: 8 },
]

export function mockRooms(movies: MovieSummary[]): MockRoom[] {
  return movies.slice(0, SAMPLES.length).map((movie, index) => {
    const { host, count, capacity } = SAMPLES[index]
    return {
      id: `sample-${index}`,
      movie,
      hostDisplayName: host,
      participantCount: count,
      capacity,
      available: count < capacity,
    }
  })
}
