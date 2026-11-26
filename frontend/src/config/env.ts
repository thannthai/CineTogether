function required(name: keyof ImportMetaEnv): string {
  const value = import.meta.env[name]
  if (!value) {
    throw new Error(`Missing ${name}. Copy frontend/.env.example to frontend/.env.local and fill it in.`)
  }
  return value
}

// Read lazily so importing this module (for example in unit tests) never fails; checkEnv() fails fast at startup.
export const env = {
  get supabaseUrl() {
    return required('VITE_SUPABASE_URL')
  },
  get supabasePublishableKey() {
    return required('VITE_SUPABASE_PUBLISHABLE_KEY')
  },
  get apiBaseUrl() {
    return required('VITE_API_BASE_URL').replace(/\/+$/, '')
  },
}

export function checkEnv() {
  void [env.supabaseUrl, env.supabasePublishableKey, env.apiBaseUrl]
}
