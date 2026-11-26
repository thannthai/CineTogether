import { useEffect, useEffectEvent, useState } from 'react'

export type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ready'; data: T }

type Result<T> = { id: string; data?: T; error?: unknown; failed: boolean }

// Loads data whenever `key` changes (put every input of `load` in the key) and again on `reload`.
export function useAsync<T>(
  load: (signal: AbortSignal) => Promise<T>,
  key: string,
): AsyncState<T> & { reload: () => void } {
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState<Result<T> | null>(null)
  const id = `${key}#${attempt}`
  const run = useEffectEvent(load)

  useEffect(() => {
    const controller = new AbortController()
    run(controller.signal)
      .then((data) => setResult({ id, data, failed: false }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) setResult({ id, error, failed: true })
      })
    return () => controller.abort()
  }, [id])

  const reload = () => setAttempt((n) => n + 1)
  // A result that belongs to an older key or attempt means the new one is still loading.
  if (result === null || result.id !== id) return { status: 'loading', reload }
  if (result.failed) return { status: 'error', error: result.error, reload }
  return { status: 'ready', data: result.data as T, reload }
}
