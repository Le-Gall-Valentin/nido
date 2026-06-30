import { useState, useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { User } from '@/entities/user'
import { ConflictError } from '../api/accountApi'
import { NetworkError, RateLimitError } from '@/shared/lib'

type Flash = { kind: 'success' | 'error'; key: string } | null

interface ProfileEditSectionProps {
  user: User
  onPatch: (partial: Partial<User>) => void
  onUpdateProfile: (username: string, email: string) => Promise<void>
}

export function ProfileEditSection({ user, onPatch, onUpdateProfile }: ProfileEditSectionProps) {
  const { t } = useTranslation('account')
  const [username, setUsername] = useState(user.username)
  const [email, setEmail] = useState(user.email)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [flash, setFlash] = useState<Flash>(null)
  const flashTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const trimmedUsername = username.trim()
  const trimmedEmail = email.trim()
  const isDirty = trimmedUsername !== user.username || trimmedEmail !== user.email
  const usernameTooShort = trimmedUsername.length > 0 && trimmedUsername.length < 3
  const usernameTooLong = trimmedUsername.length > 50
  const emailEmpty = trimmedEmail.length === 0
  const canSave = isDirty && !usernameTooShort && !usernameTooLong && !emailEmpty

  useEffect(() => {
    return () => { if (flashTimer.current) clearTimeout(flashTimer.current) }
  }, [])

  function showFlash(kind: 'success' | 'error', key: string) {
    if (flashTimer.current) clearTimeout(flashTimer.current)
    setFlash({ kind, key })
    flashTimer.current = setTimeout(() => setFlash(null), 3000)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSave || isSubmitting) return
    setIsSubmitting(true)
    try {
      await onUpdateProfile(trimmedUsername, trimmedEmail)
      onPatch({ username: trimmedUsername, email: trimmedEmail })
      showFlash('success', 'profile.success')
    } catch (error) {
      if (error instanceof ConflictError) {
        showFlash('error', 'profile.error.conflict')
      } else if (error instanceof RateLimitError) {
        showFlash('error', 'profile.error.rate_limit')
      } else if (error instanceof NetworkError) {
        showFlash('error', 'profile.error.network')
      } else {
        showFlash('error', 'profile.error.server')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleCancel() {
    setUsername(user.username)
    setEmail(user.email)
    setFlash(null)
  }

  return (
    <section id="section-profile" className="rounded-md border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-3.5 py-[10px] border-b border-border">
        <div className="text-xs font-semibold text-fg-0 tracking-tight">{t('profile.title')}</div>
        <div className="text-[11px] text-fg-2 mt-px">{t('profile.subtitle')}</div>
      </div>
      <div className="p-3.5">
        <form onSubmit={handleSubmit}>
          {usernameTooShort && <p className="text-xs text-status-orange mb-2">{t('profile.error.username_too_short')}</p>}
          {usernameTooLong && <p className="text-xs text-status-orange mb-2">{t('profile.error.username_too_long')}</p>}
          {emailEmpty && isDirty && <p className="text-xs text-status-orange mb-2">{t('profile.error.email_required')}</p>}
          <div className="flex flex-col gap-3 sm:flex-row mb-3">
            <div className="min-w-0 sm:flex-1">
              <Input
                label={t('profile.username')}
                name="username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                disabled={isSubmitting}
              />
            </div>
            <div className="min-w-0 sm:flex-[1.4]">
              <Input
                label={t('profile.email')}
                name="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                disabled={isSubmitting}
              />
            </div>
          </div>
          {flash && (
            <div
              role={flash.kind === 'success' ? 'status' : 'alert'}
              className={`mb-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
            >
              {t(flash.key)}
            </div>
          )}
          <div className="flex justify-end gap-2 mt-1">
            <Button type="button" onClick={handleCancel} disabled={!isDirty || isSubmitting} className="border-transparent bg-transparent hover:bg-bg-2 hover:border-transparent">
              {t('profile.cancel')}
            </Button>
            <Button type="submit" disabled={!canSave} isLoading={isSubmitting} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
              {t('profile.save')}
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}