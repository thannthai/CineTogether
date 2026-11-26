import { useEffect, useRef, useState, type ReactNode } from 'react'
import './dialog.css'
import { SoonContext } from './soonContext'

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
        <h2 id="soon-title">Tính năng sắp ra mắt</h2>
        <p>Xem chung cùng bạn bè đang được hoàn thiện. Hãy quay lại sớm nhé!</p>
        <button className="btn primary full" onClick={() => setOpen(false)}>
          Đã hiểu
        </button>
      </dialog>
    </SoonContext>
  )
}
