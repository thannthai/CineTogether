import { describe, expect, it } from 'vitest'
import { ApiError } from '../../shared/lib/apiError'
import { playbackMessage } from './watchErrors'

describe('playbackMessage', () => {
  it('says an episode without a source is not playable yet', () => {
    expect(playbackMessage(new ApiError(404, 'MOVIE_NOT_FOUND'))).toMatch(/chưa có nguồn phát/)
  })

  it('asks to sign in on a 401', () => {
    expect(playbackMessage(new ApiError(401, 'UNAUTHENTICATED'))).toMatch(/đăng nhập/i)
  })

  it('uses the common message for the provider being down and for network failures', () => {
    expect(playbackMessage(new ApiError(503, 'PROVIDER_UNAVAILABLE'))).toMatch(/Nguồn phim/)
    expect(playbackMessage(new TypeError('Failed to fetch'))).toMatch(/kết nối/)
  })
})
