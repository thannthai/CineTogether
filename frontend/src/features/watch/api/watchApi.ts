import { apiGetAuth } from '../../../shared/lib/api'

export type Playback = { providerKey: string; movieId: string; episodeId: string; streamUrl: string; format: 'HLS' }

// Needs a signed-in user; the link comes from a third-party server, so the browser may still fail to play it.
export const getPlayback = (providerKey: string, movieId: string, episodeId: string, signal?: AbortSignal) =>
  apiGetAuth<Playback>(
    `/movies/${encodeURIComponent(providerKey)}/${encodeURIComponent(movieId)}/playback`,
    { episodeId },
    signal,
  )
