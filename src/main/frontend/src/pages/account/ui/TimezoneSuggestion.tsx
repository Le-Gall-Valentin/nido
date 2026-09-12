import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Globe } from 'lucide-react'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { formatTimezone, shouldSuggestTimezoneChange } from '@/shared/lib'
import type { SpaceSummary } from '@/entities/space'

const DISMISSED_KEY = 'nido.timezone-suggestion.dismissed-for'

interface TimezoneSuggestionProps {
  /** Undefined while the spaces are still loading. */
  space: SpaceSummary | undefined
  browserTimezone: string
  onAccept: (timezone: string) => Promise<void>
}

/**
 * Offers to move the personal space's calendar to wherever its owner now appears to be.
 *
 * Detection suggests; it never acts. A space is created on its creator's calendar, so this only has
 * anything to say after somebody travels or moves — and the difference between those two is a
 * question only they can answer, which is why it is asked rather than assumed.
 *
 * A refusal is remembered per zone in localStorage: two weeks abroad asks once, and moving country
 * for good still gets asked about. Storage failing (private windows, blocked site data) only costs
 * the memory of the refusal, so it is not worth a fallback — the suggestion simply reappears.
 */
export function TimezoneSuggestion({ space, browserTimezone, onAccept }: TimezoneSuggestionProps) {
  const { t } = useTranslation('account')
  const [dismissedFor, setDismissedFor] = useState<string | null>(readDismissed)
  const [isSaving, setIsSaving] = useState(false)

  if (!shouldSuggestTimezoneChange({ space, browser: browserTimezone, dismissedFor })) {
    return null
  }

  // Named once and used by both strings below: the button label carries the same variable as the
  // sentence, and asking for it without passing the variable leaves "{{detected}}" on screen.
  const detected = formatTimezone(browserTimezone)

  function dismiss() {
    setDismissedFor(browserTimezone)
    try {
      localStorage.setItem(DISMISSED_KEY, browserTimezone)
    } catch {
      // Only the memory of the refusal is lost; the suggestion reappearing is the worst outcome.
    }
  }

  async function accept() {
    if (isSaving) return
    setIsSaving(true)
    try {
      await onAccept(browserTimezone)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="mb-4 rounded-2xl border border-accent-dim bg-accent-dim/30 p-4">
      <div className="flex items-start gap-3">
        <Globe className="mt-0.5 size-5 shrink-0 text-accent" aria-hidden="true" />
        <div className="min-w-0 flex-1">
          <p className="text-sm text-fg-1">
            {t('timezone_suggestion.body', {
              detected,
              current: formatTimezone(space?.timezone ?? ''),
            })}
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            <Button type="button" style={CTA_BUTTON_STYLE} onClick={accept} disabled={isSaving}>
              {t('timezone_suggestion.accept', { detected })}
            </Button>
            <Button type="button" onClick={dismiss} disabled={isSaving}>
              {t('timezone_suggestion.dismiss')}
            </Button>
          </div>
        </div>
      </div>
    </section>
  )
}

function readDismissed(): string | null {
  try {
    return localStorage.getItem(DISMISSED_KEY)
  } catch {
    return null
  }
}
