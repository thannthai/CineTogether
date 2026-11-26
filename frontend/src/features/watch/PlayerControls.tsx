import { useEffect, useRef, useState, type CSSProperties } from 'react'
import { Icon } from '../../shared/ui/Icon'
import { SPEEDS } from './playerPrefs'
import { formatTime } from './playerTime'
import { SEEK_STEP, type PlayerState } from './usePlayerState'

// Buttons to move between episodes; a null callback means there is no such episode (the button is disabled).
export type EpisodeNav = { previous: (() => void) | null; next: (() => void) | null }

type Props = {
  state: PlayerState
  onTogglePlay: () => void
  onSeekBy: (delta: number) => void
  onSeekTo: (time: number) => void
  onVolume: (volume: number) => void
  onToggleMute: () => void
  onRate: (rate: number) => void
  onTogglePip: () => void
  onToggleFullscreen: () => void
  episodeNav: EpisodeNav | null
  onMenuChange: (open: boolean) => void
}

const percent = (value: number, total: number) => (Number.isFinite(total) && total > 0 ? Math.min(100, (value / total) * 100) : 0)

// The progress bar sits above the buttons. Speed and picture-in-picture are buttons here, not in a menu.
export function PlayerControls({
  state,
  onTogglePlay,
  onSeekBy,
  onSeekTo,
  onVolume,
  onToggleMute,
  onRate,
  onTogglePip,
  onToggleFullscreen,
  episodeNav,
  onMenuChange,
}: Props) {
  const [speedOpen, setSpeedOpen] = useState(false)
  const speedRef = useRef<HTMLDivElement>(null)
  const canPip = document.pictureInPictureEnabled
  const muted = state.muted || state.volume === 0

  const setMenu = (open: boolean) => {
    setSpeedOpen(open)
    onMenuChange(open)
  }

  useEffect(() => {
    if (!speedOpen) return
    const onDown = (event: MouseEvent) => {
      if (!speedRef.current?.contains(event.target as Node)) {
        setSpeedOpen(false)
        onMenuChange(false)
      }
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [speedOpen, onMenuChange])

  const progressStyle = {
    '--played': `${percent(state.time, state.duration)}%`,
    '--buffered': `${percent(state.buffered, state.duration)}%`,
  } as CSSProperties

  return (
    <div className="player-controls">
      <div className="progress" style={progressStyle}>
        <input
          type="range"
          min={0}
          max={Number.isFinite(state.duration) ? state.duration : 0}
          step={0.1}
          value={Math.min(state.time, Number.isFinite(state.duration) ? state.duration : 0)}
          aria-label="Thanh tiến trình"
          aria-valuetext={`${formatTime(state.time)} trên ${formatTime(state.duration)}`}
          onChange={(event) => onSeekTo(Number(event.target.value))}
        />
      </div>
      <div className="times">
        <span>{formatTime(state.time)}</span>
        <span>{formatTime(state.duration)}</span>
      </div>

      <div className="control-row">
        <div className="control-group">
          <button className="ctl ctl-play" aria-label={state.playing ? 'Tạm dừng' : 'Phát'} onClick={onTogglePlay}>
            <Icon name={state.playing ? 'pause' : 'play'} size={22} />
          </button>
          <button className="ctl" aria-label={`Lùi ${SEEK_STEP} giây`} onClick={() => onSeekBy(-SEEK_STEP)}>
            <Icon name="rewind" size={22} />
            <span className="skip-label">{SEEK_STEP}</span>
          </button>
          <button className="ctl" aria-label={`Tua tới ${SEEK_STEP} giây`} onClick={() => onSeekBy(SEEK_STEP)}>
            <Icon name="forward" size={22} />
            <span className="skip-label">{SEEK_STEP}</span>
          </button>
          <div className="volume">
            <button className="ctl" aria-label={muted ? 'Bật tiếng' : 'Tắt tiếng'} onClick={onToggleMute}>
              <Icon name={muted ? 'mute' : 'volume'} size={22} />
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step={0.05}
              value={muted ? 0 : state.volume}
              aria-label="Âm lượng"
              onChange={(event) => onVolume(Number(event.target.value))}
            />
          </div>
        </div>

        <div className="control-group">
          {episodeNav && (
            <>
              <button
                className="ctl"
                aria-label="Tập trước"
                title="Tập trước"
                disabled={!episodeNav.previous}
                onClick={episodeNav.previous ?? undefined}
              >
                <Icon name="previous" size={20} />
              </button>
              <button
                className="ctl"
                aria-label="Tập tiếp theo"
                title="Tập tiếp theo"
                disabled={!episodeNav.next}
                onClick={episodeNav.next ?? undefined}
              >
                <Icon name="next" size={20} />
              </button>
            </>
          )}
          <div className="speed" ref={speedRef}>
            <button
              className="ctl ctl-text"
              aria-label="Tốc độ phát"
              aria-haspopup="menu"
              aria-expanded={speedOpen}
              onClick={() => setMenu(!speedOpen)}
            >
              <Icon name="gauge" size={20} /> <span>{state.rate}x</span>
            </button>
            {speedOpen && (
              <ul className="speed-menu" role="menu">
                {SPEEDS.map((speed) => (
                  <li key={speed} role="none">
                    <button
                      role="menuitemradio"
                      aria-checked={speed === state.rate}
                      className={speed === state.rate ? 'current' : ''}
                      onClick={() => {
                        onRate(speed)
                        setMenu(false)
                      }}
                    >
                      {speed === 1 ? 'Bình thường' : `${speed}x`}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          {canPip && (
            <button className="ctl" aria-label="Hình trong hình" aria-pressed={state.pip} onClick={onTogglePip}>
              <Icon name="pip" size={22} />
            </button>
          )}
          <button className="ctl" aria-label={state.fullscreen ? 'Thoát toàn màn hình' : 'Toàn màn hình'} onClick={onToggleFullscreen}>
            <Icon name={state.fullscreen ? 'shrink' : 'expand'} size={22} />
          </button>
        </div>
      </div>
    </div>
  )
}
