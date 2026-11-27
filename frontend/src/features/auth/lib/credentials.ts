import type { Checked } from './displayName'

const MIN_PASSWORD = 6

export function validateEmail(raw: string): Checked {
  const value = raw.trim()
  if (value === '') return { ok: false, message: 'Vui lòng nhập email.' }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return { ok: false, message: 'Email chưa đúng định dạng.' }
  return { ok: true, value }
}

// Passwords are never trimmed: spaces may be part of one.
export function validatePassword(value: string): Checked {
  if (value.length < MIN_PASSWORD) {
    return { ok: false, message: `Mật khẩu cần ít nhất ${MIN_PASSWORD} ký tự.` }
  }
  return { ok: true, value }
}

// The second entry must match the first exactly.
export function validatePasswordConfirmation(password: string, confirmation: string): Checked {
  if (confirmation === '') return { ok: false, message: 'Vui lòng nhập lại mật khẩu.' }
  if (confirmation !== password) return { ok: false, message: 'Mật khẩu nhập lại không khớp.' }
  return { ok: true, value: confirmation }
}
