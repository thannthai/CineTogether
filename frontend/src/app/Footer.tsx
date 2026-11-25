import { Logo } from './Logo'

const YEAR = new Date().getFullYear()

export function Footer() {
  return (
    <footer className="site-footer page-width">
      <div className="footer-top">
        <div>
          <Logo />
          <p>
            Mỗi bộ phim, một kết nối.
            <br />
            Khám phá điện ảnh. Chia sẻ cảm xúc.
          </p>
        </div>
      </div>
      <div className="footer-bottom">
        <span>© {YEAR} CineTogether</span>
        <span>Phim hay. Bạn tốt. Khoảnh khắc đáng nhớ.</span>
      </div>
    </footer>
  )
}
