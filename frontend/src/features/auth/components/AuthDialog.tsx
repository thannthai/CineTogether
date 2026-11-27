import { useState, type FormEvent } from 'react'
import { Icon } from '../../../shared/ui/Icon'
import { Modal } from '../../../shared/ui/Modal'
import { authErrorMessage } from '../lib/authErrors'
import { useAuth } from '../context/authContext'
import { validateEmail, validatePassword, validatePasswordConfirmation } from '../lib/credentials'
import { validateDisplayName } from '../lib/displayName'
import '../auth.css'

// 'reset' is opened by the link in the password reset email.
export type AuthMode = 'login' | 'register' | 'reset'
type Screen = AuthMode | 'forgot' | 'forgotSent' | 'confirm'

type Errors = Partial<Record<'displayName' | 'email' | 'password' | 'confirm' | 'terms', string>>

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

function EmailField({ error }: { error?: string }) {
  return (
    <Field label="Email" name="email" error={error}>
      <input
        id="email"
        name="email"
        type="email"
        autoComplete="email"
        placeholder="example@gmail.com"
        aria-invalid={!!error}
        aria-describedby={error ? 'email-error' : undefined}
      />
    </Field>
  )
}

function ConfirmField({ error }: { error?: string }) {
  return (
    <Field label="Nhập lại mật khẩu" name="confirm" error={error}>
      <PasswordInput name="confirm" placeholder="••••••••" autoComplete="new-password" invalid={!!error} />
    </Field>
  )
}

export function AuthDialog({ initialMode, onClose }: { initialMode: AuthMode; onClose: () => void }) {
  const auth = useAuth()
  const [screen, setScreen] = useState<Screen>(initialMode)
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [sentTo, setSentTo] = useState('')

  const switchTo = (next: Screen) => {
    setScreen(next)
    setErrors({})
    setBanner(null)
  }

  // Shows the found errors; returns whether the form may be sent.
  const accept = (found: Errors) => {
    setErrors(found)
    setBanner(null)
    return Object.keys(found).length === 0
  }

  const send = async (action: () => Promise<void>) => {
    setBusy(true)
    try {
      await action()
    } catch (error) {
      setBanner(authErrorMessage(error))
    }
    setBusy(false)
  }

  const submitLoginOrRegister = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const text = (name: string) => String(form.get(name) ?? '')
    const register = screen === 'register'
    const found: Errors = {}

    const email = validateEmail(text('email'))
    if (!email.ok) found.email = email.message

    if (!register) {
      if (text('password') === '') found.password = 'Vui lòng nhập mật khẩu.'
    } else {
      const password = validatePassword(text('password'))
      if (!password.ok) found.password = password.message
      else {
        const confirm = validatePasswordConfirmation(text('password'), text('confirm'))
        if (!confirm.ok) found.confirm = confirm.message
      }
    }

    const name = register ? validateDisplayName(text('displayName')) : null
    if (name && !name.ok) found.displayName = name.message
    if (register && form.get('terms') !== 'on') {
      found.terms = 'Bạn cần đồng ý với điều khoản để tiếp tục.'
    }

    if (!accept(found) || !email.ok) return
    void send(async () => {
      if (!register) {
        await auth.signIn(email.value, text('password'))
        onClose()
      } else if (name?.ok) {
        const result = await auth.signUp({ email: email.value, password: text('password'), displayName: name.value })
        if (result === 'signedIn') {
          onClose()
        } else {
          setSentTo(email.value)
          switchTo('confirm')
        }
      }
    })
  }

  const submitForgot = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const email = validateEmail(String(new FormData(event.currentTarget).get('email') ?? ''))
    if (!accept(email.ok ? {} : { email: email.message }) || !email.ok) return
    void send(async () => {
      await auth.requestPasswordReset(email.value)
      setSentTo(email.value)
      switchTo('forgotSent')
    })
  }

  const submitReset = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const text = (name: string) => String(form.get(name) ?? '')
    const found: Errors = {}
    const password = validatePassword(text('password'))
    if (!password.ok) found.password = password.message
    else {
      const confirm = validatePasswordConfirmation(text('password'), text('confirm'))
      if (!confirm.ok) found.confirm = confirm.message
    }
    if (!accept(found) || !password.ok) return
    void send(async () => {
      await auth.setNewPassword(password.value)
      onClose()
    })
  }

  if (screen === 'confirm' || screen === 'forgotSent') {
    const registered = screen === 'confirm'
    return (
      <Modal onClose={onClose} labelledBy="auth-title">
        <div className="auth-heading">
          <h2 id="auth-title">Kiểm tra email của bạn</h2>
          <p>
            {registered ? (
              <>
                Chúng tôi đã gửi liên kết xác nhận tới <strong>{sentTo}</strong>. Mở email và bấm liên kết để hoàn tất
                đăng ký, rồi đăng nhập.
              </>
            ) : (
              <>
                Nếu <strong>{sentTo}</strong> đã có tài khoản, chúng tôi vừa gửi một liên kết để đặt lại mật khẩu. Mở
                email và bấm liên kết đó.
              </>
            )}
          </p>
        </div>
        <button className="btn primary full" onClick={() => switchTo('login')}>
          Đến trang đăng nhập
        </button>
      </Modal>
    )
  }

  if (screen === 'forgot' || screen === 'reset') {
    const forgot = screen === 'forgot'
    return (
      <Modal onClose={onClose} labelledBy="auth-title">
        <div className="auth-heading">
          <h2 id="auth-title">{forgot ? 'Quên mật khẩu' : 'Đặt mật khẩu mới'}</h2>
          <p>
            {forgot
              ? 'Nhập email đã đăng ký để nhận liên kết đặt lại mật khẩu.'
              : 'Chọn một mật khẩu mới cho tài khoản của bạn.'}
          </p>
        </div>

        <form className="auth-form" onSubmit={forgot ? submitForgot : submitReset} noValidate>
          {banner && (
            <p className="form-banner" role="alert">
              {banner}
            </p>
          )}
          {forgot ? (
            <EmailField error={errors.email} />
          ) : (
            <>
              <Field label="Mật khẩu mới" name="password" error={errors.password}>
                <PasswordInput name="password" placeholder="••••••••" autoComplete="new-password" invalid={!!errors.password} />
              </Field>
              <ConfirmField error={errors.confirm} />
            </>
          )}
          <button className="btn primary full large" type="submit" disabled={busy}>
            {busy ? <span className="spinner" aria-hidden="true" /> : null}
            {forgot ? 'Gửi liên kết' : 'Lưu mật khẩu mới'}
          </button>
        </form>

        {forgot && (
          <p className="auth-switch">
            <button type="button" onClick={() => switchTo('login')}>
              Quay lại đăng nhập
            </button>
          </p>
        )}
      </Modal>
    )
  }

  const login = screen === 'login'
  return (
    <Modal onClose={onClose} labelledBy="auth-title">
      <div className="auth-heading">
        <h2 id="auth-title">{login ? 'Đăng nhập' : 'Đăng ký'}</h2>
      </div>

      <form className="auth-form" onSubmit={submitLoginOrRegister} noValidate>
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
        <EmailField error={errors.email} />
        <Field label="Mật khẩu" name="password" error={errors.password}>
          <PasswordInput
            name="password"
            placeholder="••••••••"
            autoComplete={login ? 'current-password' : 'new-password'}
            invalid={!!errors.password}
          />
        </Field>
        {login && (
          <button type="button" className="forgot-link" onClick={() => switchTo('forgot')}>
            Quên mật khẩu?
          </button>
        )}
        {!login && <ConfirmField error={errors.confirm} />}
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
