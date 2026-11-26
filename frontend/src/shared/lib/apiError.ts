export class ApiError extends Error {
  readonly status: number
  readonly code: string | null

  constructor(status: number, code: string | null) {
    super(code ?? `HTTP ${status}`)
    this.status = status
    this.code = code
  }
}

// Reads the stable `code` of an application/problem+json body; anything else gives null.
export function problemFrom(body: unknown): { code: string | null } {
  if (typeof body === 'object' && body !== null && 'code' in body) {
    const { code } = body
    if (typeof code === 'string') return { code }
  }
  return { code: null }
}

const MESSAGES: Record<string, string> = {
  PROVIDER_UNAVAILABLE: 'Nguồn phim tạm thời không khả dụng. Vui lòng thử lại sau.',
  MOVIE_NOT_FOUND: 'Không tìm thấy phim này.',
  VALIDATION_ERROR: 'Yêu cầu không hợp lệ.',
  UNAUTHENTICATED: 'Bạn cần đăng nhập để tiếp tục.',
}

// User-facing Vietnamese text; the raw error is never shown.
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code && MESSAGES[error.code]) return MESSAGES[error.code]
    if (error.status === 502 || error.status === 503) return 'Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.'
    return 'Đã có lỗi xảy ra. Vui lòng thử lại.'
  }
  if (error instanceof TypeError) return 'Không thể kết nối tới máy chủ. Kiểm tra mạng và thử lại.'
  return 'Đã có lỗi xảy ra. Vui lòng thử lại.'
}
