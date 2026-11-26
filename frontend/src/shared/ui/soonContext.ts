import { createContext, useContext } from 'react'

export const SoonContext = createContext<() => void>(() => {})

// Any "coming soon" action calls this to show the shared dialog.
export const useSoon = () => useContext(SoonContext)
