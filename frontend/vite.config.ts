import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Fixed port: the backend CORS allow-list expects http://localhost:5173.
  server: { port: 5173, strictPort: true },
  // Only pure logic is unit tested; screens are checked in the browser.
  test: { environment: 'node' },
})
