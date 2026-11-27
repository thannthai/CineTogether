import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { PlayerControls, type EpisodeNav } from './PlayerControls'
import { SEEK_STEP, usePlayerState } from '../hooks/usePlayerState'
import { useHls } from '../hooks/useHls'
import { useVideoTaps } from '../hooks/useVideoTaps'

const IDLE_MS = 2500

type Props = {
  url: string
  poster?: string | null
  onFailure: () => void
  // Null for a single movie. `next` also runs when an episode ends.
  episodeNav: EpisodeNav | null
}

export function Player({ url, poster, onFailure, episodeNav }: Props) {
  const [native, setNative] = useState(false)
  const [awake, setAwake] = useState(true)
  const [menuOpen, setMenuOpen] = useState(false)
  const idleTimer = useRef(0)

  const player = usePlayerState(() => episodeNav?.next?.())
  const { state, container, video } = player
  useHls(video, url, { onFailure, onNative: () => setNative(true) })
  const taps = useVideoTaps({
    togglePlay: player.togglePlay,
    skip: (direction) => player.seekBy(direction * SEEK_STEP),
    toggleFullscreen: player.toggleFullscreen,
  })

  const wake = () => {
    setAwake(true)
    window.clearTimeout(idleTimer.current)
    idleTimer.current = window.setTimeout(() => setAwake(false), IDLE_MS)
  }
  useEffect(() => {
    container.current?.focus({ preventScroll: true })
    return () => window.clearTimeout(idleTimer.current)
  }, [container])

  // The controls stay while paused or while a menu is open; they hide after a pause in activity.
  const visible = awake || !state.playing || menuOpen

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.metaKey || event.ctrlKey || event.altKey) return
    const target = event.target as HTMLElement
    // On the volume slider the arrows change the volume; on a button Space presses the button.
    if (target.matches('input[aria-label="Âm lượng"]')) return
    if (event.key === ' ' && target.closest('button')) return
    const handled = {
      ' ': player.togglePlay,
      k: player.togglePlay,
      arrowleft: () => player.seekBy(-SEEK_STEP),
      arrowright: () => player.seekBy(SEEK_STEP),
      m: player.toggleMute,
      f: player.toggleFullscreen,
    }[event.key.toLowerCase()]
    if (!handled) return
    event.preventDefault()
    handled()
    wake()
  }

  return (
    <div
      ref={container}
      className={`player-surface ${visible ? '' : 'idle'}`}
      tabIndex={0}
      onKeyDown={onKeyDown}
      onMouseMove={wake}
      onTouchStart={wake}
    >
      <video
        ref={video}
        className="player-video"
        autoPlay
        playsInline
        controls={native}
        poster={poster ?? undefined}
        onPointerDown={native ? undefined : taps.handlers.onPointerDown}
        onClick={native ? undefined : taps.handlers.onClick}
        onDoubleClick={native ? undefined : taps.handlers.onDoubleClick}
        onError={onFailure}
      />
      {taps.flash && (
        <div key={taps.flash.id} className={`skip-flash ${taps.flash.side}`} aria-hidden="true">
          {taps.flash.side === 'left' ? '−' : '+'}
          {SEEK_STEP} giây
        </div>
      )}
      {!native && (
        <PlayerControls
          state={state}
          onTogglePlay={player.togglePlay}
          onSeekBy={player.seekBy}
          onSeekTo={player.seekTo}
          onVolume={player.setVolume}
          onToggleMute={player.toggleMute}
          onRate={player.setRate}
          onTogglePip={player.togglePip}
          onToggleFullscreen={player.toggleFullscreen}
          episodeNav={episodeNav}
          onMenuChange={setMenuOpen}
        />
      )}
    </div>
  )
}
