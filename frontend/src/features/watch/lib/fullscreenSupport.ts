export type FullscreenMethod = 'container' | 'video' | 'none'

const can = (target: object, name: string) => typeof (target as Record<string, unknown>)[name] === 'function'

// The player container goes full screen when the browser allows it, so our controls stay on screen. iPhone Safari
// only allows it for the <video> itself (its own player and controls take over); some browsers allow neither.
export function fullscreenMethod(container: object, video: object): FullscreenMethod {
  if (can(container, 'requestFullscreen')) return 'container'
  if (can(video, 'webkitEnterFullscreen')) return 'video'
  return 'none'
}
