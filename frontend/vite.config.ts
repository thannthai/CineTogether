import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Fixed port: the backend CORS allow-list expects http://localhost:5173.
  server: { port: 5173, strictPort: true },
  // hls.js is only imported on demand; listing it here pre-bundles it at startup, so the dev server does not
  // re-optimize in the middle of a page visit and break the dynamic import.
  optimizeDeps: { include: ['hls.js'] },
  // Only pure logic is unit tested; screens are checked in the browser.
  test: { environment: 'node' },
})
