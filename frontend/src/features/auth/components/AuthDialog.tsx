import { useState, type FormEvent } from 'react'
import { Icon } from '../../../shared/ui/Icon'
import { Modal } from '../../../shared/ui/Modal'
import { authErrorMessage } from '../lib/authErrors'
import { useAuth } from '../context/authContext'
import { validateEmail, validatePassword } from '../lib/credentials'
import { validateDisplayName } from '../lib/displayName'
import '../auth.css'

export type AuthMode = 'login' | 'register'

type Errors = Partial<Record<'displayName' | 'email' | 'password' | 'terms', string>>

function Field({
  label,
  name,
  error,
  children,
}: {
  label: string
  name: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className={`field ${error ? 'invalid' : ''}`}>
      <label htmlFor={name}>{label}</label>
      {children}
      {error && (
        <p className="field-error" id={`${name}-error`} role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

function PasswordInput({ name, placeholder, autoComplete, invalid }: { name: string; placeholder: string; autoComplete: string; invalid: boolean }) {
  const [shown, setShown] = useState(false)
  return (
    <div className="password-input">
      <input
        id={name}
        name={name}
        type={shown ? 'text' : 'password'}
        placeholder={placeholder}
        autoComplete={autoComplete}
        aria-invalid={invalid}
        aria-describedby={invalid ? `${name}-error` : undefined}
      />
      <button type="button" className="icon-btn" aria-label={shown ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShown(!shown)}>
        <Icon name={shown ? 'eyeOff' : 'eye'} size={18} />
      </button>
    </div>
  )
}

export function AuthDialog({ initialMode, onClose }: { initialMode: AuthMode; onClose: () => void }) {
  const auth = useAuth()
  const [mode, setMode] = useState<AuthMode | 'confirm'>(initialMode)
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [sentTo, setSentTo] = useState('')

  const switchTo = (next: AuthMode) => {
    setMode(next)
    setErrors({})
    setBanner(null)
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const text = (name: string) => String(form.get(name) ?? '')
    const found: Errors = {}

    const email = validateEmail(text('email'))
    if (!email.ok) found.email = email.message

    if (mode === 'login') {
      if (text('password') === '') found.password = 'Vui lòng nhập mật khẩu.'
    } else {
      const password = validatePassword(text('password'))
      if (!password.ok) found.password = password.message
    }

    const name = mode === 'register' ? validateDisplayName(text('displayName')) : null
    if (name && !name.ok) found.displayName = name.message
    if (mode === 'register' && form.get('terms') !== 'on') {
      found.terms = 'Bạn cần đồng ý với điều khoản để tiếp tục.'
    }

    setErrors(found)
    setBanner(null)
    if (Object.keys(found).length > 0 || !email.ok) return

    setBusy(true)
    try {
      if (mode === 'login') {
        await auth.signIn(email.value, text('password'))
        onClose()
      } else if (name?.ok) {
        const result = await auth.signUp({ email: email.value, password: text('password'), displayName: name.value })
        if (result === 'signedIn') {
          onClose()
        } else {
          setSentTo(email.value)
          setMode('confirm')
        }
      }
    } catch (error) {
      setBanner(authErrorMessage(error))
    }
    setBusy(false)
  }

  if (mode === 'confirm') {
    return (
      <Modal onClose={onClose} labelledBy="auth-title">
        <div className="auth-heading">
          <h2 id="auth-title">Kiểm tra email của bạn</h2>
          <p>
            Chúng tôi đã gửi liên kết xác nhận tới <strong>{sentTo}</strong>. Mở email và bấm liên kết để hoàn tất đăng ký,
            rồi đăng nhập.
          </p>
        </div>
        <button className="btn primary full" onClick={() => switchTo('login')}>
          Đến trang đăng nhập
        </button>
      </Modal>
    )
  }

  const login = mode === 'login'
  return (
    <Modal onClose={onClose} labelledBy="auth-title">
      <div className="auth-heading">
        <h2 id="auth-title">{login ? 'Đăng nhập' : 'Đăng ký'}</h2>
      </div>

      <form className="auth-form" onSubmit={submit} noValidate>
        {banner && (
          <p className="form-banner" role="alert">
            {banner}
          </p>
        )}
        {!login && (
          <Field label="Tên hiển thị" name="displayName" error={errors.displayName}>
            <input
              id="displayName"
              name="displayName"
              autoComplete="nickname"
              placeholder="Bạn muốn được gọi là gì?"
              aria-invalid={!!errors.displayName}
              aria-describedby={errors.displayName ? 'displayName-error' : undefined}
            />
          </Field>
        )}
        <Field label="Email" name="email" error={errors.email}>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="example@gmail.com"
            aria-invalid={!!errors.email}
            aria-describedby={errors.email ? 'email-error' : undefined}
          />
        </Field>
        <Field label="Mật khẩu" name="password" error={errors.password}>
          <PasswordInput
            name="password"
            placeholder="••••••••"
            autoComplete={login ? 'current-password' : 'new-password'}
            invalid={!!errors.password}
          />
        </Field>
        {!login && (
          <div className={`field ${errors.terms ? 'invalid' : ''}`}>
            <label className="checkbox-label">
              <input type="checkbox" name="terms" />
              <span>Tôi đồng ý với điều khoản và chính sách quyền riêng tư.</span>
            </label>
            {errors.terms && (
              <p className="field-error" role="alert">
                {errors.terms}
              </p>
            )}
          </div>
        )}
        <button className="btn primary full large" type="submit" disabled={busy}>
          {busy ? <span className="spinner" aria-hidden="true" /> : null}
          {login ? 'Đăng nhập' : 'Đăng ký'}
        </button>
      </form>

      <p className="auth-switch">
        {login ? 'Chưa có tài khoản?' : 'Đã có tài khoản?'}{' '}
        <button type="button" onClick={() => switchTo(login ? 'register' : 'login')}>
          {login ? 'Đăng ký ngay' : 'Đăng nhập'}
        </button>
      </p>
    </Modal>
  )
}
