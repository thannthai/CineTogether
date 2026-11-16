import { useEffect, useState } from 'react'
import { env } from './config/env'

type BackendStatus = { state: 'checking' } | { state: 'reachable'; httpStatus: number } | { state: 'unreachable' }

// Temporary setup check, to be removed once real screens exist (Authentication & User Profile milestone).
// Any HTTP response, even 401 for an unauthenticated request, proves the backend is reachable and CORS allows us.
function useBackendStatus(): BackendStatus {
  const [status, setStatus] = useState<BackendStatus>({ state: 'checking' })

  useEffect(() => {
    const controller = new AbortController()
    fetch(`${env.apiBaseUrl}/me`, { signal: controller.signal })
      .then((response) => setStatus({ state: 'reachable', httpStatus: response.status }))
      .catch((error: unknown) => {
        if (!(error instanceof DOMException && error.name === 'AbortError')) {
          setStatus({ state: 'unreachable' })
        }
      })
    return () => controller.abort()
  }, [])

  return status
}

export default function App() {
  const backend = useBackendStatus()

  return (
    <main>
      <h1>CineTogether</h1>
      <p>Development environment is running.</p>
      <ul>
        <li>Supabase URL configured: {env.supabaseUrl ? 'yes' : 'no'}</li>
        <li>Supabase publishable key configured: {env.supabasePublishableKey ? 'yes' : 'no'}</li>
        <li>
          Backend ({env.apiBaseUrl}):{' '}
          {backend.state === 'checking' && 'checking…'}
          {backend.state === 'unreachable' && 'unreachable (backend down or CORS blocked)'}
          {backend.state === 'reachable' && `reachable (HTTP ${backend.httpStatus})`}
        </li>
      </ul>
    </main>
  )
}
