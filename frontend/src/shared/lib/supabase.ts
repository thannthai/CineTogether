import { createClient, type SupabaseClient } from '@supabase/supabase-js'
import { env } from '../../config/env'

let client: SupabaseClient | undefined

// Created on first use, so importing code that only needs the API client never requires the env values.
export function getSupabase(): SupabaseClient {
  client ??= createClient(env.supabaseUrl, env.supabasePublishableKey)
  return client
}

// The current access token, refreshed by the Supabase client when it is about to expire; null when signed out.
export async function getAccessToken(): Promise<string | null> {
  const { data } = await getSupabase().auth.getSession()
  return data.session?.access_token ?? null
}
