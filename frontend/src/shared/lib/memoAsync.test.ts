import { describe, expect, it, vi } from 'vitest'
import { memoAsync } from './memoAsync'

describe('memoAsync', () => {
  it('loads a key once and shares the result', async () => {
    const load = vi.fn(async (key: string) => `value of ${key}`)
    const get = memoAsync(load)

    expect(await get('a')).toBe('value of a')
    expect(await get('a')).toBe('value of a')
    expect(load).toHaveBeenCalledTimes(1)
  })

  it('keeps keys apart', async () => {
    const load = vi.fn(async (key: string) => key)
    const get = memoAsync(load)

    await get('a')
    await get('b')
    expect(load).toHaveBeenCalledTimes(2)
  })

  it('shares a request that is still running', async () => {
    const load = vi.fn(async (key: string) => key)
    const get = memoAsync(load)

    await Promise.all([get('a'), get('a')])
    expect(load).toHaveBeenCalledTimes(1)
  })

  it('does not remember a failure, so the next call tries again', async () => {
    const load = vi.fn<(key: string) => Promise<string>>()
    load.mockRejectedValueOnce(new Error('down')).mockResolvedValueOnce('ok')
    const get = memoAsync(load)

    await expect(get('a')).rejects.toThrow('down')
    expect(await get('a')).toBe('ok')
    expect(load).toHaveBeenCalledTimes(2)
  })
})
