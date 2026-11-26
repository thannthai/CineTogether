import { Outlet, ScrollRestoration } from 'react-router-dom'
import './app.css'
import { Footer } from './Footer'
import { AuthProvider } from '../features/auth'
import { SoonProvider } from '../shared/ui/Soon'
import { Header } from './Header'

export function AppLayout() {
  return (
    <AuthProvider>
      <SoonProvider>
        <div className="app-shell">
          <Header />
          <main className="app-main">
            <Outlet />
          </main>
          <Footer />
          <ScrollRestoration />
        </div>
      </SoonProvider>
    </AuthProvider>
  )
}
