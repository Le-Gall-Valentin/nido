import { useEffect, useRef } from 'react'
import { useAuth } from '../model/authStoreContext'
import { forgetPendingLanguage, keepPendingLanguage, readPendingLanguage } from '../model/pendingLanguage'
import { accountLanguageApi, type IAccountLanguageApi } from '@/entities/user'
import { useLanguage } from '@/shared/lib'

interface Props {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: IAccountLanguageApi
}

/**
 * Keeps the language on screen and the account's language in step:
 *
 * - at sign-in (or when a session is restored), the account's language wins over what this device
 *   detected — someone who reads Nido in English on their phone reads it in English here too — and
 *   as soon as the server names the account, so the 2FA proposal in between already speaks it;
 * - an account that never recorded a language records the one this session detected;
 * - a change made while signed in (Preferences) is recorded on the account.
 *
 * The account's language is also what the server writes mails in, when nobody is there to ask.
 * A failed save is kept on this device for that account and not retried in the session; at the
 * account's next sign-in here it is the latest choice, so it wins over the older recorded language and
 * is saved again — then forgotten once the server takes it.
 * Renders nothing; the app mounts it inside AuthProvider, which it reads, and under LanguageProvider.
 */
export function AccountLanguageSync({ api = accountLanguageApi }: Props) {
  const user = useAuth((s) => s.user)
  const signingIn = useAuth((s) => s.signingIn)
  const patchUser = useAuth((s) => s.patchUser)
  const { language, setLanguage } = useLanguage()
  const appliedFor = useRef<string | null>(null)
  // The account is known before the sign-in is finished — the 2FA proposal comes in between.
  const account = user ?? signingIn

  useEffect(() => {
    if (!account) {
      appliedFor.current = null
      return
    }
    const recorded = account.language ?? null
    if (appliedFor.current !== account.id) {
      appliedFor.current = account.id
      const pending = readPendingLanguage(account.id)
      const wanted = pending ?? recorded
      if (wanted !== null && wanted !== language) {
        setLanguage(wanted)
        return
      }
      // The recorded language is on screen already, and nothing waits to be saved.
      if (pending === null && recorded !== null) return
    } else if (recorded === language) {
      return
    }
    // Saving needs the session, which exists once the sign-in is finished.
    if (!user) return
    const userId = user.id
    const chosen = language
    void api.saveLanguage(chosen).then(
      () => {
        forgetPendingLanguage(userId)
        patchUser({ language: chosen })
      },
      () => keepPendingLanguage(userId, chosen),
    )
  }, [account, user, language, api, patchUser, setLanguage])

  return null
}
