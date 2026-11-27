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

  it('follows the shape of a public room: a name, a host, a count within the capacity, and whether there is room', () => {
    for (const room of mockRooms(Array.from({ length: 6 }, (_, i) => movie(`m${i}`)))) {
      expect(room.name).toBeTruthy()
      expect(room.hostDisplayName).toBeTruthy()
      expect(room.participantCount).toBeGreaterThanOrEqual(1)
      expect(room.participantCount).toBeLessThanOrEqual(room.capacity)
      expect(room.available).toBe(room.status !== 'ended' && room.participantCount < room.capacity)
    }
  })

  it('shows every status: live, upcoming and ended rooms', () => {
    const statuses = new Set(mockRooms(Array.from({ length: 6 }, (_, i) => movie(`m${i}`))).map((room) => room.status))
    expect(statuses).toEqual(new Set(['live', 'upcoming', 'ended']))
  })

  it('gives an episode only to a series', () => {
    const series = { ...movie('s'), type: 'SERIES' as const }
    expect(mockRooms([movie('a')])[0].episode).toBeNull()
    expect(mockRooms([series])[0].episode).toBeGreaterThanOrEqual(1)
  })

  it('includes a full room, to show that state', () => {
    const rooms = mockRooms(Array.from({ length: 6 }, (_, i) => movie(`m${i}`)))
    expect(rooms.some((room) => room.status === 'live' && room.participantCount === room.capacity)).toBe(true)
  })
})
