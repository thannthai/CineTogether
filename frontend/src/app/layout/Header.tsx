import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  browseHref,
  GENRES,
  MENU_COUNTRIES,
  SearchPalette,
  type FilterOption,
} from '../../features/catalog'
import { AccountMenu, useAuth, useAuthDialog } from '../../features/auth'
import { useTheme } from '../../shared/lib/theme'
import { Icon } from '../../shared/ui/Icon'
import { SoonTag } from '../../shared/ui/SoonTag'
import { activeNav } from './activeNav'
import { Logo } from './Logo'
import { NAV_LINKS as LINKS } from './navLinks'


type MenuKey = 'genre' | 'country'

const MENUS: Record<MenuKey, { label: string; items: FilterOption[]; toFilter: (slug: string) => string }> = {
  genre: { label: 'Thể loại', items: GENRES, toFilter: (slug) => browseHref({ genre: slug }) },
  country: {
    label: 'Quốc gia',
    items: MENU_COUNTRIES,
    toFilter: (slug) => browseHref({ country: slug }),
  },
}
const MENU_KEYS = Object.keys(MENUS) as MenuKey[]

export function Header() {
  const location = useLocation()
  const params = new URLSearchParams(location.search)
  const active = activeNav(location.pathname, params)
  const currentSlug: Record<MenuKey, string | null> = {
    genre: params.get('genre'),
    country: params.get('country'),
  }

  const { theme, toggle: toggleTheme } = useTheme()
  const auth = useAuth()
  const { openLogin, openRegister } = useAuthDialog()
  const themeLabel = theme === 'dark' ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối'

  const [menu, setMenu] = useState<MenuKey | null>(null)
  const [drawer, setDrawer] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [section, setSection] = useState<MenuKey | null>(null)
  const closeTimer = useRef(0)
  const navRef = useRef<HTMLElement>(null)

  const closeAll = () => {
    setMenu(null)
    setDrawer(false)
  }
  const openSearch = () => {
    closeAll()
    setSearchOpen(true)
  }

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setSearchOpen(true)
        return
      }
      if (event.key !== 'Escape') return
      if (menu) navRef.current?.querySelector<HTMLElement>(`[data-menu="${menu}"]`)?.focus()
      closeAll()
    }
    const onDown = (event: MouseEvent) => {
      if (!navRef.current?.contains(event.target as Node)) setMenu(null)
    }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onDown)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.removeEventListener('mousedown', onDown)
    }
  }, [menu])

  useEffect(() => {
    document.body.style.overflow = drawer ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [drawer])

  const hover = (key: MenuKey | null) => {
    window.clearTimeout(closeTimer.current)
    if (key) setMenu(key)
    else closeTimer.current = window.setTimeout(() => setMenu(null), 140)
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <Logo />
        <nav className="main-nav" ref={navRef} aria-label="Điều hướng chính">
          {LINKS.map(({ key, label, to }) => (
            <Link
              key={key}
              to={to}
              className={`nav-item ${active === key ? 'active' : ''}`}
              aria-current={active === key ? 'page' : undefined}
              onClick={closeAll}
            >
              {label}
            </Link>
          ))}
          {MENU_KEYS.map((key) => {
            const config = MENUS[key]
            return (
              <div
                key={key}
                className="nav-dropdown"
                onMouseEnter={() => hover(key)}
                onMouseLeave={() => hover(null)}
              >
                <button
                  data-menu={key}
                  className={`nav-item ${active === key ? 'active' : ''} ${menu === key ? 'open' : ''}`}
                  aria-expanded={menu === key}
                  aria-haspopup="true"
                  onClick={() => setMenu(menu === key ? null : key)}
                >
                  {config.label}
                  <Icon name="chevronDown" size={13} className="nav-chevron" />
                </button>
                <div className={`mega-menu ${menu === key ? 'open' : ''}`} inert={menu !== key}>
                  <div className="mega-grid">
                    {config.items.map((item) => (
                      <Link
                        key={item.slug}
                        to={config.toFilter(item.slug)}
                        className={currentSlug[key] === item.slug ? 'selected' : ''}
                        onClick={closeAll}
                      >
                        {item.name}
                      </Link>
                    ))}
                  </div>
                  <Link className="mega-all" to={browseHref()} onClick={closeAll}>
                    Xem tất cả <Icon name="arrow" size={14} />
                  </Link>
                </div>
              </div>
            )
          })}
          <Link
            to="/together"
            className={`nav-item ${active === 'together' ? 'active' : ''}`}
            aria-current={active === 'together' ? 'page' : undefined}
            onClick={closeAll}
          >
            Xem chung <SoonTag />
          </Link>
        </nav>

        <div className="header-tools">
          <button
            className="tool-btn"
            aria-label="Tìm kiếm (Cmd hoặc Ctrl + K)"
            data-tip="Tìm kiếm  ⌘K"
            aria-haspopup="dialog"
            onClick={openSearch}
          >
            <Icon name="search" size={17} />
          </button>
          <button
            className={`tool-btn theme-btn is-${theme}`}
            aria-label={themeLabel}
            data-tip={themeLabel}
            onClick={toggleTheme}
          >
            <span className="theme-icon moon" aria-hidden="true">
              <Icon name="moon" size={17} />
            </span>
            <span className="theme-icon sun" aria-hidden="true">
              <Icon name="sun" size={18} />
            </span>
          </button>
          {auth.status === 'signedIn' && <AccountMenu />}
          {auth.status === 'signedOut' && (
            <div className="header-auth">
              <button className="login-link" onClick={openLogin}>
                Đăng nhập
              </button>
              <button className="btn primary signup-btn" onClick={openRegister}>
                Đăng ký
              </button>
            </div>
          )}
          <button
            className="icon-btn menu-toggle"
            aria-label={drawer ? 'Đóng menu' : 'Mở menu'}
            aria-expanded={drawer}
            onClick={() => setDrawer(!drawer)}
          >
            <Icon name={drawer ? 'close' : 'menu'} size={22} />
          </button>
        </div>
      </div>

      <div className={`nav-drawer-backdrop ${drawer ? 'open' : ''}`} onClick={() => setDrawer(false)} />
      <aside className={`nav-drawer ${drawer ? 'open' : ''}`} inert={!drawer} aria-label="Menu">
        <div className="drawer-head">
          <Logo />
          <button className="icon-btn" aria-label="Đóng menu" onClick={() => setDrawer(false)}>
            <Icon name="close" size={22} />
          </button>
        </div>
        <button className="drawer-search" onClick={openSearch}>
          <Icon name="search" size={17} />
          <span>Tìm phim bạn muốn xem...</span>
        </button>
        <div className="drawer-links">
          {LINKS.map(({ key, label, to }) => (
            <Link key={key} to={to} className={active === key ? 'active' : ''} onClick={closeAll}>
              {label}
            </Link>
          ))}
          {MENU_KEYS.map((key) => (
            <div key={key} className={`drawer-group ${section === key ? 'open' : ''}`}>
              <button
                className={active === key ? 'active' : ''}
                aria-expanded={section === key}
                onClick={() => setSection(section === key ? null : key)}
              >
                {MENUS[key].label}
                <Icon name="chevronDown" size={15} />
              </button>
              <div className="drawer-chips" inert={section !== key}>
                <div>
                  {MENUS[key].items.map((item) => (
                    <Link
                      key={item.slug}
                      to={MENUS[key].toFilter(item.slug)}
                      className={currentSlug[key] === item.slug ? 'selected' : ''}
                      onClick={closeAll}
                    >
                      {item.name}
                    </Link>
                  ))}
                  <Link className="chip-all" to={browseHref()} onClick={closeAll}>
                    Xem tất cả
                  </Link>
                </div>
              </div>
            </div>
          ))}
          <Link to="/together" className={active === 'together' ? 'active' : ''} onClick={closeAll}>
            <span>
              Xem chung <SoonTag />
            </span>
          </Link>
        </div>
        {auth.status === 'signedOut' && (
          <div className="drawer-auth">
            <button
              className="btn secondary"
              onClick={() => {
                closeAll()
                openLogin()
              }}
            >
              Đăng nhập
            </button>
            <button
              className="btn primary"
              onClick={() => {
                closeAll()
                openRegister()
              }}
            >
              Đăng ký
            </button>
          </div>
        )}
      </aside>
      {searchOpen && <SearchPalette onClose={() => setSearchOpen(false)} />}
    </header>
  )
}
