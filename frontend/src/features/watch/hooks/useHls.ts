import { useEffect, useEffectEvent, type RefObject } from 'react'

type Handlers = { onFailure: () => void; onNative: () => void }

// Attaches an HLS stream to a <video> with hls.js, which is loaded only when a stream is opened.
// Browsers without Media Source Extensions (iPhone Safari) play the stream themselves: `onNative` tells the
// caller to show the browser's own controls.
export function useHls(video: RefObject<HTMLVideoElement | null>, url: string, { onFailure, onNative }: Handlers) {
  const fail = useEffectEvent(onFailure)
  const showNative = useEffectEvent(onNative)

  useEffect(() => {
    const element = video.current
    if (!element) return
    let cancelled = false
    let hls: { destroy: () => void } | undefined

    // hls.js first: recent Chrome says it can play HLS itself but often cannot.
    import('hls.js')
      .then(({ default: Hls }) => {
        if (cancelled) return
        if (!Hls.isSupported()) {
          if (element.canPlayType('application/vnd.apple.mpegurl')) {
            showNative()
            element.src = url
          } else {
            fail()
          }
          return
        }
        const player = new Hls()
        hls = player
        player.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) fail()
        })
        player.loadSource(url)
        player.attachMedia(element)
      })
      .catch(fail)

    return () => {
      cancelled = true
      hls?.destroy()
      element.removeAttribute('src')
      element.load()
    }
  }, [url, video])
}
