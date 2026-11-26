import { env } from '../../config/env'
import { ApiError, problemFrom } from './apiError'
import { getAccessToken } from './supabase'

type Query = Record<string, string | number | undefined>
type Options = { query?: Query; body?: unknown; signal?: AbortSignal; auth?: boolean }

// The token is sent only when `auth` is set: the backend answers 401 to a bad token even on public endpoints.
async function request<T>(method: string, path: string, { query = {}, body, signal, auth }: Options): Promise<T> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== '') params.set(key, String(value))
  }
  const search = params.toString()

  const headers: Record<string, string> = {}
  if (auth) {
    const token = await getAccessToken()
    if (!token) throw new ApiError(401, 'UNAUTHENTICATED')
    headers.Authorization = `Bearer ${token}`
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(`${env.apiBaseUrl}${path}${search ? `?${search}` : ''}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })
  if (!response.ok) {
    const problem: unknown = await response.json().catch(() => null)
    throw new ApiError(response.status, problemFrom(problem).code)
  }
  return (await response.json()) as T
}

export const apiGet = <T>(path: string, query: Query = {}, signal?: AbortSignal) =>
  request<T>('GET', path, { query, signal })

export const apiGetAuth = <T>(path: string, signal?: AbortSignal) =>
  request<T>('GET', path, { signal, auth: true })

export const apiPatchAuth = <T>(path: string, body: unknown, signal?: AbortSignal) =>
  request<T>('PATCH', path, { body, signal, auth: true })
