type AuthFailure = { code?: string; name?: string; status?: number } | undefined | null

const GENERIC = 'Đã có lỗi xảy ra. Vui lòng thử lại.'

const BY_CODE: Record<string, string> = {
  invalid_credentials: 'Email hoặc mật khẩu không đúng.',
  email_not_confirmed: 'Email chưa được xác nhận. Hãy kiểm tra hộp thư của bạn.',
  user_already_exists: 'Email này đã được đăng ký.',
  email_exists: 'Email này đã được đăng ký.',
  weak_password: 'Mật khẩu quá yếu, hãy chọn mật khẩu khác.',
  same_password: 'Mật khẩu mới phải khác mật khẩu cũ.',
  over_request_rate_limit: 'Bạn thử quá nhiều lần. Vui lòng đợi một lát rồi thử lại.',
  over_email_send_rate_limit: 'Bạn thử quá nhiều lần. Vui lòng đợi một lát rồi thử lại.',
}

// Vietnamese text for a failure of Supabase Auth; the raw message is never shown.
export function authErrorMessage(error: unknown): string {
  if (error instanceof TypeError) return 'Không thể kết nối. Kiểm tra mạng và thử lại.'
  const failure = error as AuthFailure
  if (failure?.name === 'AuthRetryableFetchError') return 'Không thể kết nối. Kiểm tra mạng và thử lại.'
  if (failure?.code && BY_CODE[failure.code]) return BY_CODE[failure.code]
  return GENERIC
}
