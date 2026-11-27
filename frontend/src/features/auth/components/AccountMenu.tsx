import { useEffect, useRef, useState } from 'react'
import { Icon } from '../../../shared/ui/Icon'
import { useAuth } from '../context/authContext'
import { ProfileDialog } from './ProfileDialog'
import '../auth.css'

// The avatar in the header with the name, "Hồ sơ" and "Đăng xuất".
export function AccountMenu() {
  const auth = useAuth()
  const [open, setOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const initial = auth.displayName.trim().slice(0, 1).toUpperCase()

  useEffect(() => {
    if (!open) return
    const onDown = (event: MouseEvent) => {
      if (!ref.current?.contains(event.target as Node)) setOpen(false)
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  return (
    <div className="account" ref={ref}>
      <button
        className="avatar"
        aria-label="Tài khoản của bạn"
        aria-expanded={open}
        aria-haspopup="menu"
        onClick={() => setOpen(!open)}
      >
        {initial}
      </button>
      <div className={`account-menu ${open ? 'open' : ''}`} role="menu" inert={!open}>
        <div className="account-identity">
          <span className="avatar">{initial}</span>
          <div>
            <strong>{auth.displayName}</strong>
            {auth.email && <small>{auth.email}</small>}
          </div>
        </div>
        <button
          role="menuitem"
          onClick={() => {
            setOpen(false)
            setProfileOpen(true)
          }}
        >
          <Icon name="user" size={16} /> Hồ sơ
        </button>
        <button
          role="menuitem"
          onClick={() => {
            setOpen(false)
            void auth.signOut()
          }}
        >
          <Icon name="logout" size={16} /> Đăng xuất
        </button>
      </div>
      {profileOpen && <ProfileDialog onClose={() => setProfileOpen(false)} />}
    </div>
  )
}
