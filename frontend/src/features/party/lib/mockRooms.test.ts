import { describe, expect, it } from 'vitest'
import { mockRooms } from './mockRooms'

const movie = (id: string) => ({ providerKey: 'kkphim', movieId: id, title: id, type: 'MOVIE' as const })

describe('mockRooms', () => {
  it('makes one sample room per movie, up to six', () => {
    expect(mockRooms([movie('a'), movie('b')])).toHaveLength(2)
    expect(mockRooms(Array.from({ length: 9 }, (_, i) => movie(`m${i}`)))).toHaveLength(6)
    expect(mockRooms([])).toEqual([])
  })

  it('is the same every time, so the page does not change on every render', () => {
    const movies = [movie('a'), movie('b'), movie('c')]
    expect(mockRooms(movies)).toEqual(mockRooms(movies))
  })

  it('follows the shape of a public room: a host, a count within the capacity, and whether there is room', () => {
    for (const room of mockRooms(Array.from({ length: 6 }, (_, i) => movie(`m${i}`)))) {
      expect(room.hostDisplayName).toBeTruthy()
      expect(room.participantCount).toBeGreaterThanOrEqual(1)
      expect(room.participantCount).toBeLessThanOrEqual(room.capacity)
      expect(room.available).toBe(room.participantCount < room.capacity)
    }
  })

  it('includes a full room, to show that state', () => {
    const rooms = mockRooms(Array.from({ length: 6 }, (_, i) => movie(`m${i}`)))
    expect(rooms.some((room) => !room.available)).toBe(true)
  })
})
