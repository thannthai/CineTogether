// Remembers the promise per key, so repeated or overlapping calls share one request.
// A failed request is forgotten so the next call tries again.
export function memoAsync<T>(load: (key: string) => Promise<T>): (key: string) => Promise<T> {
  const cache = new Map<string, Promise<T>>()
  return (key) => {
    let promise = cache.get(key)
    if (!promise) {
      promise = load(key).catch((error: unknown) => {
        cache.delete(key)
        throw error
      })
      cache.set(key, promise)
    }
    return promise
  }
}
