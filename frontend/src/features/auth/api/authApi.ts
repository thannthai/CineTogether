import { apiGetAuth, apiPatchAuth } from '../../../shared/lib/api'

export type Profile = { id: string; displayName: string; createdAt: string }

// The first authenticated request makes the backend create the profile.
export const getMe = (signal?: AbortSignal) => apiGetAuth<Profile>('/me', {}, signal)

export const updateDisplayName = (displayName: string) => apiPatchAuth<Profile>('/me', { displayName })
