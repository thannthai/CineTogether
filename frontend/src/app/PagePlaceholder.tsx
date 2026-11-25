import { Link } from 'react-router-dom'

// Temporary screen until its feature is ported; each slice replaces one of these.
export function PagePlaceholder({ title, note }: { title: string; note?: string }) {
  return (
    <section className="page-placeholder page-width">
      <h1>{title}</h1>
      {note && <p>{note}</p>}
    </section>
  )
}

export function NotFoundPage() {
  return (
    <section className="page-placeholder page-width">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn này không tồn tại hoặc đã được di chuyển.</p>
      <Link className="btn primary" to="/">
        Về trang chủ
      </Link>
    </section>
  )
}
