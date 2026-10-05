import { useTranslation } from 'react-i18next'
import fr from '../locales/fr.json'
import type { MailTestFailedError } from '../model/errors'

// A code is known when it has a wording. One the server adds before the pages do is worded as unknown,
// never shown as a raw translation key.
const PROBLEMS = new Set(Object.keys(fr.problem))
const FAILURES = new Set(Object.keys(fr.mail_failure))

/** How the setup screen and the settings page word a refused setting and a failed test mail. */
export function useSettingWording() {
  const { t } = useTranslation('instanceSettings')
  return {
    problem: (code: string) => t(PROBLEMS.has(code) ? `problem.${code}` : 'problem.unknown'),
    mailFailure: (error: MailTestFailedError) => [
      t(FAILURES.has(error.reason) ? `mail_failure.${error.reason}` : 'mail_failure.failed'),
      error.serverReply,
    ].filter(Boolean).join(' — '),
  }
}
