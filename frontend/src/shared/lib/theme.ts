import { useCallback, useEffect, useState } from 'react'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'cinetogether-theme'
const root = () => document.documentElement

// index.html sets data-theme before first paint, so the attribute is the source of truth.
const current = (): Theme => (root().getAttribute('data-theme') === 'dark' ? 'dark' : 'light')

function readSaved(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(current)

  const apply = useCallback((next: Theme, animate: boolean) => {
    const el = root()
    if (animate && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      el.classList.add('theme-switching')
      window.setTimeout(() => el.classList.remove('theme-switching'), 320)
    }
    el.setAttribute('data-theme', next)
    setTheme(next)
  }, [])

  const toggle = useCallback(() => {
    const next: Theme = current() === 'dark' ? 'light' : 'dark'
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // Storage unavailable: the choice lasts for this session only.
    }
    apply(next, true)
  }, [apply])

  // Follow system changes until the user picks a theme explicitly.
  useEffect(() => {
    const query = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (event: MediaQueryListEvent) => {
      const saved = readSaved()
      if (saved !== 'light' && saved !== 'dark') apply(event.matches ? 'dark' : 'light', true)
    }
    query.addEventListener('change', onChange)
    return () => query.removeEventListener('change', onChange)
  }, [apply])

  return { theme, toggle }
}
