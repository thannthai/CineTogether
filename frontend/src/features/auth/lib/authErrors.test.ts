import { describe, expect, it } from 'vitest'
import { authErrorMessage } from './authErrors'

describe('authErrorMessage', () => {
  it('maps the known Supabase codes', () => {
    expect(authErrorMessage({ code: 'invalid_credentials' })).toMatch(/không đúng/)
    expect(authErrorMessage({ code: 'email_not_confirmed' })).toMatch(/xác nhận/)
    expect(authErrorMessage({ code: 'user_already_exists' })).toMatch(/đã được đăng ký/)
    expect(authErrorMessage({ code: 'email_exists' })).toMatch(/đã được đăng ký/)
    expect(authErrorMessage({ code: 'weak_password' })).toMatch(/Mật khẩu/)
    expect(authErrorMessage({ code: 'over_request_rate_limit' })).toMatch(/quá nhiều/)
  })

  it('reports a network failure', () => {
    expect(authErrorMessage({ name: 'AuthRetryableFetchError', status: 0 })).toMatch(/kết nối/)
    expect(authErrorMessage(new TypeError('Failed to fetch'))).toMatch(/kết nối/)
  })

  it('falls back to a generic message and never leaks the raw text', () => {
    const message = authErrorMessage({ code: 'something_new', message: 'secret internal detail' })
    expect(message).toMatch(/Đã có lỗi/)
    expect(message).not.toMatch(/secret/)
    expect(authErrorMessage(undefined)).toMatch(/Đã có lỗi/)
  })
})
