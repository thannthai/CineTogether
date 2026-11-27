import { Logo } from './Logo'

const YEAR = new Date().getFullYear()

export function Footer() {
  return (
    <footer className="site-footer page-width">
      <div className="footer-hero">
        <Logo />
        <p className="footer-slogan">Mỗi bộ phim, một kết nối.</p>
        <p className="footer-about">
          Xem cùng bạn bè, gặp gỡ những người cùng yêu phim, và để những bộ phim đưa chúng ta đến gần nhau hơn.
        </p>
      </div>
      <div className="footer-bottom">© {YEAR} CineTogether</div>
    </footer>
  )
}
