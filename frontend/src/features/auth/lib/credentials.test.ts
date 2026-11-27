import { describe, expect, it } from 'vitest'
import { validateEmail, validatePassword } from './credentials'

describe('validateEmail', () => {
  it('accepts a usual address and trims it', () => {
    expect(validateEmail(' ban@example.com ')).toEqual({ ok: true, value: 'ban@example.com' })
  })

  it('rejects an empty or malformed address', () => {
    for (const bad of ['', 'ban', 'ban@', '@example.com', 'ban@example', 'ban @example.com']) {
      expect(validateEmail(bad)).toMatchObject({ ok: false })
    }
  })
})

describe('validatePassword', () => {
  it('needs at least six characters', () => {
    expect(validatePassword('12345')).toMatchObject({ ok: false })
    expect(validatePassword('123456')).toEqual({ ok: true, value: '123456' })
  })

  it('does not trim a password', () => {
    expect(validatePassword('  abcd  ')).toEqual({ ok: true, value: '  abcd  ' })
  })
})
