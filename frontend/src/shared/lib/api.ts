import { env } from '../../config/env'
import { ApiError, problemFrom } from './apiError'

type Query = Record<string, string | number | undefined>

export async function apiGet<T>(path: string, query: Query = {}, signal?: AbortSignal): Promise<T> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') params.set(key, String(value))
  }
  const search = params.toString()
  const response = await fetch(`${env.apiBaseUrl}${path}${search ? `?${search}` : ''}`, { signal })
  if (!response.ok) {
    const body: unknown = await response.json().catch(() => null)
    throw new ApiError(response.status, problemFrom(body).code)
  }
  return (await response.json()) as T
}
