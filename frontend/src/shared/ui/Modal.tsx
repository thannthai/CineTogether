import { useEffect, useRef, type ReactNode } from 'react'
import { Icon } from './Icon'
import './dialog.css'

type Props = { onClose: () => void; labelledBy: string; children: ReactNode }

// A native modal dialog: it traps focus and closes with Escape. Mount it only while it should be open,
// so a form inside always starts empty.
export function Modal({ onClose, labelledBy, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    ref.current?.showModal()
  }, [])

  return (
    <dialog
      ref={ref}
      className="dialog modal"
      aria-labelledby={labelledBy}
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <button className="icon-btn modal-close" aria-label="Đóng" onClick={onClose}>
        <Icon name="close" size={20} />
      </button>
      {children}
    </dialog>
  )
}
