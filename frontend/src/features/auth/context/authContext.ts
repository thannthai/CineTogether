import { createContext, useContext } from 'react'

export type SignUpResult = 'signedIn' | 'confirmEmail'

export type Auth = {
  status: 'loading' | 'signedOut' | 'signedIn'
  displayName: string
  email: string
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: { email: string; password: string; displayName: string }) => Promise<SignUpResult>
  requestPasswordReset: (email: string) => Promise<void>
  setNewPassword: (password: string) => Promise<void>
  signOut: () => Promise<void>
  rename: (displayName: string) => Promise<void>
}

export const AuthContext = createContext<Auth | null>(null)

export function useAuth(): Auth {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth must be used inside AuthProvider')
  return auth
}

export type AuthDialogControls = { openLogin: () => void; openRegister: () => void }

export const AuthDialogContext = createContext<AuthDialogControls>({ openLogin: () => {}, openRegister: () => {} })

// Any screen that needs a signed-in user opens the login or register dialog through this.
export const useAuthDialog = () => useContext(AuthDialogContext)
