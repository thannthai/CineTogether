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
        <Link className="page-step" to={hrefFor(prev)} aria-label="Trang trước">
          <Icon name="chevronLeft" size={16} />
        </Link>
      ) : (
        <span className="page-step disabled" aria-disabled="true" aria-label="Trang trước">
          <Icon name="chevronLeft" size={16} />
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
        <Link className="page-step" to={hrefFor(next)} aria-label="Trang sau">
          <Icon name="chevron" size={16} />
        </Link>
      ) : (
        <span className="page-step disabled" aria-disabled="true" aria-label="Trang sau">
          <Icon name="chevron" size={16} />
        </span>
      )}
    </nav>
  )
}
