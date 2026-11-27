import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Icon } from './Icon'
import './dialog.css'
import { SoonContext } from './soonContext'

// A small popcorn bucket in a dashed frame with two sparkles; decorative only.
function SoonArt() {
  return (
    <span className="soon-art" aria-hidden="true">
      <svg viewBox="0 0 48 48" width="44" height="44">
        <g className="soon-puffs">
          <circle cx="15" cy="17" r="6" />
          <circle cx="24" cy="12" r="7" />
          <circle cx="33" cy="17" r="6" />
          <circle cx="24" cy="19" r="6" />
        </g>
        <path className="soon-bucket" d="M12 22h24l-3 21H15z" />
        <path className="soon-stripes" d="M19 24l1 17M24 24v17M29 24l-1 17" />
        <circle className="soon-eye" cx="20.5" cy="31" r="1.3" />
        <circle className="soon-eye" cx="27.5" cy="31" r="1.3" />
      </svg>
      <svg className="soon-spark soon-spark-a" viewBox="-8 -8 16 16" width="14" height="14">
        <path d="M0-7 2-2 7 0 2 2 0 7-2 2-7 0-2-2Z" />
      </svg>
      <svg className="soon-spark soon-spark-b" viewBox="-8 -8 16 16" width="10" height="10">
        <path d="M0-7 2-2 7 0 2 2 0 7-2 2-7 0-2-2Z" />
      </svg>
    </span>
  )
}

export function SoonProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const dialogRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <SoonContext value={() => setOpen(true)}>
      {children}
      <dialog
        ref={dialogRef}
        className="dialog"
        aria-labelledby="soon-title"
        onClose={() => setOpen(false)}
        onClick={(event) => {
          if (event.target === event.currentTarget) setOpen(false)
        }}
      >
        <button className="icon-btn modal-close" aria-label="Đóng" onClick={() => setOpen(false)}>
          <Icon name="close" size={20} />
        </button>
        <SoonArt />
        <h2 id="soon-title">Tính năng sắp ra mắt</h2>
        <p>Xem chung cùng bạn bè đang được hoàn thiện. Hãy quay lại sớm nhé!</p>
        <button className="btn primary full" onClick={() => setOpen(false)}>
          Đã hiểu
        </button>
      </dialog>
    </SoonContext>
  )
}
