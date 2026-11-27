import { useState, type FormEvent } from 'react'
import { ApiError, errorMessage } from '../../../shared/lib/apiError'
import { Modal } from '../../../shared/ui/Modal'
import { useAuth } from '../context/authContext'
import { validateDisplayName } from '../lib/displayName'
import '../auth.css'

export function ProfileDialog({ onClose }: { onClose: () => void }) {
  const auth = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const name = validateDisplayName(String(new FormData(event.currentTarget).get('displayName') ?? ''))
    if (!name.ok) {
      setError(name.message)
      return
    }
    setError(null)
    setBusy(true)
    try {
      await auth.rename(name.value)
      onClose()
    } catch (failure) {
      setError(
        failure instanceof ApiError && failure.code === 'VALIDATION_ERROR'
          ? 'Tên hiển thị không hợp lệ.'
          : errorMessage(failure),
      )
      setBusy(false)
    }
  }

  return (
    <Modal onClose={onClose} labelledBy="profile-title">
      <div className="auth-heading">
        <h2 id="profile-title">Hồ sơ của bạn</h2>
        <p>{auth.email}</p>
      </div>
      <form className="auth-form" onSubmit={submit} noValidate>
        <div className={`field ${error ? 'invalid' : ''}`}>
          <label htmlFor="profile-name">Tên hiển thị</label>
          <input
            id="profile-name"
            name="displayName"
            defaultValue={auth.displayName}
            autoComplete="nickname"
            aria-invalid={!!error}
            aria-describedby={error ? 'profile-name-error' : undefined}
          />
          {error && (
            <p className="field-error" id="profile-name-error" role="alert">
              {error}
            </p>
          )}
        </div>
        <button className="btn primary full large" type="submit" disabled={busy}>
          {busy ? <span className="spinner" aria-hidden="true" /> : null}
          Lưu thay đổi
        </button>
      </form>
    </Modal>
  )
}
