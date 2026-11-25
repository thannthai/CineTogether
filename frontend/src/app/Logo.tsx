import { Link } from 'react-router-dom'
import { Icon } from '../shared/ui/Icon'

export function Logo() {
  return (
    <Link className="logo" to="/" aria-label="CineTogether, trang chủ">
      <span className="logo-symbol">
        <Icon name="play" size={19} />
      </span>
      <span>
        Cine<span className="logo-accent">Together</span>
      </span>
    </Link>
  )
}
