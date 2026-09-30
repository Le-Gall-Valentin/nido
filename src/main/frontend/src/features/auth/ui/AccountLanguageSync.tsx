import { useEffect, useRef } from 'react'
import { useAuth } from '../model/authStoreContext'
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
 * A failed save keeps the language on this device and is not retried. An account with no recorded
 * language gets it recorded at the next sign-in; an account that already has one has that language
 * applied again at the next sign-in, so the choice made on the failed save is lost.
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
      if (recorded !== null) {
        if (recorded !== language) setLanguage(recorded)
        return
      }
    } else if (recorded === language) {
      return
    }
    // Saving needs the session, which exists once the sign-in is finished.
    if (!user) return
    void api.saveLanguage(language).then(
      () => patchUser({ language }),
      () => { /* kept on this device only; not retried, and lost at the next sign-in if the account already has a language */ },
    )
  }, [account, user, language, api, patchUser, setLanguage])

  return null
}
