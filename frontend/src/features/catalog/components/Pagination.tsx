import { Link } from 'react-router-dom'
import { Icon } from '../../../shared/ui/Icon'
import { pageWindow } from '../lib/browse/browseView'

type Props = {
  current: number
  hasNext: boolean
  hrefFor: (page: number) => string
}

export function Pagination({ current, hasNext, hrefFor }: Props) {
  const { items, prev, next } = pageWindow(current, hasNext)
  if (prev === null && next === null) return null

  return (
    <nav className="pagination" aria-label="Phân trang">
      {prev !== null ? (
        <Link className="page-step" to={hrefFor(prev)}>
          <Icon name="chevronLeft" size={14} /> Trước
        </Link>
      ) : (
        <span className="page-step disabled" aria-disabled="true">
          <Icon name="chevronLeft" size={14} /> Trước
        </span>
      )}
      <div className="page-numbers">
        {items.map((item, index) =>
          item === '…' ? (
            <span key={`gap-${index}`} className="page-gap" aria-hidden="true">
              …
            </span>
          ) : (
            <Link
              key={item}
              to={hrefFor(item)}
              className={item === current ? 'current' : ''}
              aria-current={item === current ? 'page' : undefined}
            >
              {item}
            </Link>
          ),
        )}
      </div>
      {next !== null ? (
        <Link className="page-step" to={hrefFor(next)}>
          Sau <Icon name="chevron" size={14} />
        </Link>
      ) : (
        <span className="page-step disabled" aria-disabled="true">
          Sau <Icon name="chevron" size={14} />
        </span>
      )}
    </nav>
  )
}
