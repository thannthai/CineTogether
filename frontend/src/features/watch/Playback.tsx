import { useState } from 'react'
import { useAsync } from '../../shared/lib/useAsync'
import { Icon } from '../../shared/ui/Icon'
import { getPlayback } from './api'
import { Player } from './Player'
import type { EpisodeNav } from './PlayerControls'
import { playbackMessage } from './watchErrors'

type Props = {
  providerKey: string
  movieId: string
  episodeId: string
  poster?: string | null
  episodeNav: EpisodeNav | null
}

function Message({ title, text, action }: { title: string; text: string; action?: { label: string; run: () => void } }) {
  return (
    <div className="player-state" role="alert">
      <Icon name="alert" size={26} />
      <h2>{title}</h2>
      <p>{text}</p>
      {action && (
        <button className="btn primary" onClick={action.run}>
          <Icon name="refresh" size={15} /> {action.label}
        </button>
      )}
    </div>
  )
}

// Resolves the stream of one episode (a signed-in call) and plays it.
export function Playback({ providerKey, movieId, episodeId, poster, episodeNav }: Props) {
  const source = useAsync(
    (signal) => getPlayback(providerKey, movieId, episodeId, signal),
    `playback:${providerKey}/${movieId}/${episodeId}`,
  )
  const [failure, setFailure] = useState<{ url: string; attempt: number } | null>(null)
  const [attempt, setAttempt] = useState(0)

  if (source.status === 'loading') {
    return (
      <div className="player-state" role="status">
        <span className="spinner" aria-hidden="true" />
        <p>Đang chuẩn bị phim...</p>
      </div>
    )
  }
  if (source.status === 'error') {
    return <Message title="Không thể phát tập này" text={playbackMessage(source.error)} action={{ label: 'Thử lại', run: source.reload }} />
  }

  const { streamUrl } = source.data
  if (failure && failure.url === streamUrl && failure.attempt === attempt) {
    return (
      <Message
        title="Không thể phát nội dung này"
        text="Nguồn phát của tập này đang gặp sự cố. Hãy thử lại hoặc chọn tập khác."
        action={{ label: 'Thử lại', run: () => setAttempt(attempt + 1) }}
      />
    )
  }
  return (
    <Player
      key={`${streamUrl}#${attempt}`}
      url={streamUrl}
      poster={poster}
      episodeNav={episodeNav}
      onFailure={() => setFailure({ url: streamUrl, attempt })}
    />
  )
}
