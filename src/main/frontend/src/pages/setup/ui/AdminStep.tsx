import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { isValidEmail, passwordProblem, useLanguage, usernameProblem } from '@/shared/lib'
import { Button, CTA_BUTTON_STYLE, Input, PasswordInput } from '@/shared/ui'
import type { SetupAdmin } from '../model/types'
import { ACTIONS_CLASS, FIELD_ERROR_CLASS, LEAD_CLASS, TITLE_CLASS } from './styles'

interface Props {
  initial: SetupAdmin
  onNext: (admin: SetupAdmin) => void
}

export function AdminStep({ initial, onNext }: Props) {
  const { t } = useTranslation('setup')
  const { language } = useLanguage()
  const [username, setUsername] = useState(initial.username)
  const [email, setEmail] = useState(initial.email)
  const [password, setPassword] = useState(initial.password)
  const [confirm, setConfirm] = useState(initial.password)
  const [visible, setVisible] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})

  function submit(event: FormEvent) {
    event.preventDefault()
    const found: Record<string, string> = {}
    const nameProblem = usernameProblem(username)
    if (nameProblem) found.username = `admin.username_${nameProblem}`
    if (!isValidEmail(email.trim())) found.email = 'admin.email_invalid'
    const secretProblem = passwordProblem(password)
    if (secretProblem) found.password = `admin.password_${secretProblem}`
    if (confirm !== password) found.confirm = 'admin.confirm_mismatch'
    setErrors(found)
    if (Object.keys(found).length === 0) {
      onNext({ username: username.trim(), email: email.trim(), password, language })
    }
  }

  const error = (field: string) => errors[field] && <p className={FIELD_ERROR_CLASS}>{t(errors[field])}</p>

  return (
    <form onSubmit={submit} noValidate>
      <h1 className={TITLE_CLASS}>{t('admin.title')}</h1>
      <p className={LEAD_CLASS}>{t('admin.lead')}</p>
      <div className="flex flex-col gap-4">
        <div>
          <Input label={t('admin.username')} name="admin-username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" maxLength={50} />
          {error('username')}
        </div>
        <div>
          <Input label={t('admin.email')} name="admin-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" maxLength={254} />
          {error('email')}
        </div>
        <div>
          <PasswordInput label={t('admin.password')} name="admin-password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" visible={visible} onVisibleChange={setVisible} />
          {error('password')}
        </div>
        <div>
          <PasswordInput label={t('admin.confirm')} name="admin-confirm" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" visible={visible} onVisibleChange={setVisible} />
          {error('confirm')}
        </div>
      </div>
      <div className={ACTIONS_CLASS}>
        <span />
        <Button type="submit" className="border-transparent" style={CTA_BUTTON_STYLE}>{t('action.next')}</Button>
      </div>
    </form>
  )
}
