const MAX_LENGTH = 80
const ZERO_WIDTH_JOINER = '‍'

export type Checked = { ok: true; value: string } | { ok: false; message: string }

// Mirrors the backend rule (DisplayNames) so a bad name is caught before the request.
// The backend still has the final say and answers 400 VALIDATION_ERROR.
export function validateDisplayName(raw: string): Checked {
  const value = raw.trim()
  // Control characters and invisible format characters (zero-width space, right-to-left override, line
  // breaks) are rejected, but the zero-width joiner stays: it builds emoji sequences.
  const hasBad = Array.from(value).some(
    (char) => char !== ZERO_WIDTH_JOINER && /[\p{Cc}\p{Cf}]/u.test(char),
  )
  const hasVisible = Array.from(value).some((char) => char !== ZERO_WIDTH_JOINER && /[^\s\p{Zs}\p{Zl}\p{Zp}]/u.test(char))

  if (!hasVisible) return { ok: false, message: 'Vui lòng nhập tên hiển thị.' }
  if (hasBad) return { ok: false, message: 'Tên hiển thị có ký tự không hợp lệ.' }
  if (Array.from(value).length > MAX_LENGTH) {
    return { ok: false, message: `Tên hiển thị tối đa ${MAX_LENGTH} ký tự.` }
  }
  return { ok: true, value }
}
