import { describe, expect, it } from 'vitest'
import { validateDisplayName } from './displayName'

const ok = (value: string) => ({ ok: true, value })

describe('validateDisplayName', () => {
  it('accepts a normal name and trims it', () => {
    expect(validateDisplayName('  Minh Nguyễn  ')).toEqual(ok('Minh Nguyễn'))
  })

  it('rejects a blank name', () => {
    for (const blank of ['', '   ', ' 　']) {
      expect(validateDisplayName(blank)).toMatchObject({ ok: false })
    }
  })

  it('counts characters, not UTF-16 units: an emoji is one', () => {
    expect(validateDisplayName('a'.repeat(80))).toEqual(ok('a'.repeat(80)))
    expect(validateDisplayName('😀'.repeat(80))).toEqual(ok('😀'.repeat(80)))
    expect(validateDisplayName('a'.repeat(81))).toMatchObject({ ok: false })
  })

  it('rejects control and invisible format characters', () => {
    for (const bad of ['a\nb', 'a\tb', 'a​b', 'a‮b', 'a\u0000b']) {
      expect(validateDisplayName(bad)).toMatchObject({ ok: false })
    }
  })

  it('allows the zero-width joiner that builds emoji sequences', () => {
    const family = '👨‍👩‍👧'
    expect(validateDisplayName(family)).toEqual(ok(family))
  })

  it('rejects a name with nothing visible', () => {
    expect(validateDisplayName('‍')).toMatchObject({ ok: false })
  })

  it('gives a Vietnamese message', () => {
    const result = validateDisplayName('')
    expect(result.ok === false && result.message).toMatch(/tên hiển thị/i)
  })
})
