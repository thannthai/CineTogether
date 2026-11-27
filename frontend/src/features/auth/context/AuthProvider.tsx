import { useEffect, useState, type ReactNode } from 'react'
import type { Session } from '@supabase/supabase-js'
import { getSupabase } from '../../../shared/lib/supabase'
import { useAsync } from '../../../shared/lib/useAsync'
import { getMe, updateDisplayName, type Profile } from '../api/authApi'
import { AuthContext, AuthDialogContext, type Auth } from './authContext'
import { AuthDialog, type AuthMode } from '../components/AuthDialog'

const emailName = (email: string) => email.split('@')[0] ?? ''

// Keeps the Supabase session and the backend profile, and shows the login and register dialog.
export function AuthProvider({ children }: { children: ReactNode }) {
  // undefined while the saved session is still being read.
  const [session, setSession] = useState<Session | null | undefined>(undefined)
  const [dialog, setDialog] = useState<AuthMode | null>(null)
  const [renamed, setRenamed] = useState<{ userId: string; profile: Profile } | null>(null)

  useEffect(() => {
    const supabase = getSupabase()
    void supabase.auth.getSession().then(({ data }) => setSession((current) => current ?? data.session))
    const { data } = supabase.auth.onAuthStateChange((_event, next) => setSession(next))
    return () => data.subscription.unsubscribe()
  }, [])

  const userId = session?.user.id
  const profile = useAsync(
    (signal) => (userId ? getMe(signal) : Promise.resolve(null)),
    `me:${userId ?? 'none'}`,
  )
  const loaded = profile.status === 'ready' ? profile.data : null
  const current = renamed && renamed.userId === userId ? renamed.profile : loaded

  const email = session?.user.email ?? ''
  const metadataName = session?.user.user_metadata?.display_name
  const displayName =
    current?.displayName ?? (typeof metadataName === 'string' && metadataName ? metadataName : emailName(email))

  const auth: Auth = {
    status: session === undefined ? 'loading' : session ? 'signedIn' : 'signedOut',
    displayName,
    email,
    async signIn(emailValue, password) {
      const { error } = await getSupabase().auth.signInWithPassword({ email: emailValue, password })
      if (error) throw error
    },
    async signUp({ email: emailValue, password, displayName: name }) {
      const { data, error } = await getSupabase().auth.signUp({
        email: emailValue,
        password,
        options: { data: { display_name: name } },
      })
      if (error) throw error
      // With email confirmation on, an address that already exists comes back as a user with no identities.
      if (data.user && data.user.identities?.length === 0) throw { code: 'user_already_exists' }
      return data.session ? 'signedIn' : 'confirmEmail'
    },
    async signOut() {
      const { error } = await getSupabase().auth.signOut()
      if (error) throw error
    },
    async rename(name) {
      const updated = await updateDisplayName(name)
      if (userId) setRenamed({ userId, profile: updated })
    },
  }

  return (
    <AuthContext value={auth}>
      <AuthDialogContext value={{ openLogin: () => setDialog('login'), openRegister: () => setDialog('register') }}>
        {children}
        {dialog && <AuthDialog initialMode={dialog} onClose={() => setDialog(null)} />}
      </AuthDialogContext>
    </AuthContext>
  )
}
