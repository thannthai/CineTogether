import { useCallback, useEffect, useEffectEvent, useRef, useState } from 'react'
import { loadPrefs, savePrefs } from '../lib/playerPrefs'
import { seekTarget } from '../lib/playerTime'

export const SEEK_STEP = 5

export type PlayerState = {
  playing: boolean
  time: number
  duration: number
  buffered: number
  volume: number
  muted: boolean
  rate: number
  fullscreen: boolean
  pip: boolean
}

const bufferedEnd = (video: HTMLVideoElement) => {
  const { buffered, currentTime } = video
  for (let i = 0; i < buffered.length; i++) {
    if (buffered.start(i) <= currentTime && currentTime <= buffered.end(i)) return buffered.end(i)
  }
  return currentTime
}

// Owns the refs of the player's <video> and container, mirrors the video into React state and gives the
// actions the controls need.
export function usePlayerState(onEnded: () => void) {
  const video = useRef<HTMLVideoElement>(null)
  const container = useRef<HTMLDivElement>(null)
  // Always the latest callback, without making the listeners below start over when it changes.
  const handleEnded = useEffectEvent(onEnded)
  const [state, setState] = useState<PlayerState>({
    playing: false,
    time: 0,
    duration: NaN,
    buffered: 0,
    volume: 1,
    muted: false,
    rate: 1,
    fullscreen: false,
    pip: false,
  })

  useEffect(() => {
    const element = video.current
    if (!element) return

    // The volume and the speed chosen earlier carry over to the next episode.
    const prefs = loadPrefs()
    element.volume = prefs.volume
    element.muted = prefs.muted
    // Loading a stream resets the speed to the default one, so the default is set too.
    element.defaultPlaybackRate = prefs.rate
    element.playbackRate = prefs.rate

    const sync = () =>
      setState((previous) => ({
        ...previous,
        playing: !element.paused && !element.ended,
        time: element.currentTime,
        duration: element.duration,
        buffered: bufferedEnd(element),
        volume: element.volume,
        muted: element.muted,
        rate: element.playbackRate,
      }))
    const remember = () => savePrefs({ volume: element.volume, muted: element.muted, rate: element.playbackRate })
    const setPip = (pip: boolean) => setState((previous) => ({ ...previous, pip }))
    const enterPip = () => setPip(true)
    const leavePip = () => setPip(false)
    const ended = () => {
      sync()
      handleEnded()
    }

    const events = ['play', 'pause', 'timeupdate', 'durationchange', 'loadedmetadata', 'progress', 'seeked', 'volumechange', 'ratechange']
    events.forEach((name) => element.addEventListener(name, sync))
    element.addEventListener('volumechange', remember)
    element.addEventListener('ratechange', remember)
    element.addEventListener('ended', ended)
    element.addEventListener('enterpictureinpicture', enterPip)
    element.addEventListener('leavepictureinpicture', leavePip)
    sync()
    return () => {
      events.forEach((name) => element.removeEventListener(name, sync))
      element.removeEventListener('volumechange', remember)
      element.removeEventListener('ratechange', remember)
      element.removeEventListener('ended', ended)
      element.removeEventListener('enterpictureinpicture', enterPip)
      element.removeEventListener('leavepictureinpicture', leavePip)
    }
  }, [video])

  useEffect(() => {
    const onChange = () =>
      setState((previous) => ({ ...previous, fullscreen: document.fullscreenElement === container.current }))
    document.addEventListener('fullscreenchange', onChange)
    return () => document.removeEventListener('fullscreenchange', onChange)
  }, [container])

  const togglePlay = useCallback(() => {
    const element = video.current
    if (!element) return
    if (element.paused) void element.play().catch(() => {})
    else element.pause()
  }, [video])

  const seekBy = useCallback(
    (delta: number) => {
      const element = video.current
      if (element) element.currentTime = seekTarget(element.currentTime, delta, element.duration)
    },
    [video],
  )

  const seekTo = useCallback(
    (time: number) => {
      const element = video.current
      if (element) element.currentTime = time
    },
    [video],
  )

  const setVolume = useCallback(
    (volume: number) => {
      const element = video.current
      if (!element) return
      element.volume = volume
      element.muted = volume === 0
    },
    [video],
  )

  const toggleMute = useCallback(() => {
    const element = video.current
    if (element) element.muted = !element.muted
  }, [video])

  const setRate = useCallback(
    (rate: number) => {
      const element = video.current
      if (!element) return
      element.defaultPlaybackRate = rate
      element.playbackRate = rate
    },
    [video],
  )

  // The container goes full screen, not the <video>, so our controls stay on screen.
  const toggleFullscreen = useCallback(() => {
    // The browser may refuse (no user gesture, or full screen turned off): the player simply stays as it is.
    if (document.fullscreenElement) void document.exitFullscreen().catch(() => {})
    else void container.current?.requestFullscreen().catch(() => {})
  }, [container])

  const togglePip = useCallback(() => {
    const element = video.current
    if (!element) return
    if (document.pictureInPictureElement) void document.exitPictureInPicture()
    else void element.requestPictureInPicture().catch(() => {})
  }, [video])

  return { video, container, state, togglePlay, seekBy, seekTo, setVolume, toggleMute, setRate, toggleFullscreen, togglePip }
}
