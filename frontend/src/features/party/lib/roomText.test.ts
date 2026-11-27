import { describe, expect, it } from 'vitest'
import type { MockRoom } from './mockRooms'
import { joinNote, movieLine } from './roomText'

const room = (patch: Partial<MockRoom> = {}, movie: Partial<MockRoom['movie']> = {}): MockRoom => ({
  id: 'r',
  name: 'Phòng',
  status: 'live',
  movie: { providerKey: 'kkphim', movieId: 'm', title: 'Phim', type: 'MOVIE', ...movie },
  hostDisplayName: 'Lan',
  participantCount: 2,
  capacity: 6,
  available: true,
  episode: null,
  ...patch,
})

describe('movieLine', () => {
  it('joins the year and the episode of a series, and shows no genre', () => {
    expect(movieLine(room({ episode: 3 }, { releaseYear: 2024, genres: ['Cổ trang', 'Hành động'] }))).toBe('2024 · Tập 3')
  })

  it('leaves out what is unknown', () => {
    expect(movieLine(room({}, { releaseYear: 2024 }))).toBe('2024')
    expect(movieLine(room())).toBe('')
  })
})

describe('joinNote', () => {
  it('says whether there is room, or that the room is over', () => {
    expect(joinNote(room())).toBe('Còn chỗ tham gia')
    expect(joinNote(room({ available: false }))).toBe('Phòng đã đầy')
    expect(joinNote(room({ status: 'ended', available: false }))).toBe('Phòng đã kết thúc')
    expect(joinNote(room({ status: 'upcoming' }))).toBe('Còn chỗ tham gia')
  })
})
