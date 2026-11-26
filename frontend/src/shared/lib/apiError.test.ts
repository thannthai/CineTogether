import { describe, expect, it } from 'vitest'
import { ApiError, errorMessage, problemFrom } from './apiError'

describe('problemFrom', () => {
  it('reads the machine-readable code from a problem+json body', () => {
    expect(problemFrom({ status: 503, code: 'PROVIDER_UNAVAILABLE', title: 'x' }).code).toBe(
      'PROVIDER_UNAVAILABLE',
    )
  })

  it('tolerates bodies that are not problem+json', () => {
    expect(problemFrom(null).code).toBeNull()
    expect(problemFrom('oops').code).toBeNull()
    expect(problemFrom({ code: 42 }).code).toBeNull()
  })
})

describe('errorMessage', () => {
  it('maps known codes to Vietnamese messages', () => {
    expect(errorMessage(new ApiError(503, 'PROVIDER_UNAVAILABLE'))).toMatch(/Nguồn phim/)
    expect(errorMessage(new ApiError(404, 'MOVIE_NOT_FOUND'))).toMatch(/Không tìm thấy/)
  })

  it('falls back on the status when the code is unknown', () => {
    expect(errorMessage(new ApiError(503, null))).toMatch(/tạm thời/)
    expect(errorMessage(new ApiError(500, null))).toMatch(/Đã có lỗi/)
  })

  it('reports a network failure when fetch itself throws', () => {
    expect(errorMessage(new TypeError('Failed to fetch'))).toMatch(/kết nối/)
  })

  it('never leaks the raw error text', () => {
    expect(errorMessage(new Error('secret stack detail'))).not.toMatch(/secret/)
  })
})
