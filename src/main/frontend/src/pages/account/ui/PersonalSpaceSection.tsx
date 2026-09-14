import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { timezoneChoices } from '@/shared/lib'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { SpaceSummary } from '@/entities/space'

interface PersonalSpaceSectionProps {
  /** Undefined while the spaces are still loading. */
  space: SpaceSummary | undefined
  onSave: (timezone: string) => Promise<void>
}

/**
 * The personal space's settings, which exist here because it has nowhere else to live.
 *
 * A personal space has no detail page: there is one member, so no roster to manage, and its name,
 * colour and glyph are deliberately fixed — there was simply nothing in it to edit. Its calendar is
 * the first thing that is genuinely its own and genuinely worth changing, and somebody who moves
 * country has to be able to reach it.
 *
 * Put in account settings rather than given a space page of its own because that is where the rest
 * of "things about me" already is, and because a page holding one field is not a page.
 */
export function PersonalSpaceSection({ space, onSave }: PersonalSpaceSectionProps) {
  const { t } = useTranslation('account')
  /**
   * What the user is choosing, until the space itself says otherwise.
   *
   * <p>Cleared by the effect below whenever the space's zone changes, rather than compared to anything:
   * the banner above this section can move the calendar on its own, and a draft that outlived that
   * showed the earlier pick with its save button live — one click from silently undoing the move that
   * had just been accepted.
   *
   * <p>Two cleverer versions of this were wrong, and the browser caught both while the unit tests
   * passed. Comparing the draft to the zone it was made against fails when the space returns to
   * exactly that zone — pick Paris on Toronto, save, then accept the banner's move back to Toronto,
   * and the spent Paris draft matches again and comes back to life. Tracking the previously rendered
   * zone fails the same way, since it too ends up equal. What actually distinguishes "still choosing"
   * from "the space moved under me" is that the space changed at all, which is what an effect keyed on
   * it observes and no derivation can.
   */
  const [draft, setDraft] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [failed, setFailed] = useState(false)

  const spaceTimezone = space?.timezone
  useEffect(() => {
    setDraft(null)
  }, [spaceTimezone])

  if (!space) {
    return null
  }

  const selected = draft ?? space.timezone
  const isDirty = selected !== space.timezone

  async function handleSave() {
    if (!isDirty || isSaving) return
    setIsSaving(true)
    setFailed(false)
    try {
      await onSave(selected)
    } catch {
      // The button releases whether the save worked or not, so silence here is indistinguishable from
      // success: same form, same values, and the calendar unchanged on the server.
      setFailed(true)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-bg-1 p-5">
      <h2 className="text-[17px] font-semibold text-fg-0">{t('personal_space.title')}</h2>
      <p className="mt-1 text-sm text-fg-2">{t('personal_space.subtitle')}</p>

      <div className="mt-4 flex flex-col gap-1.5">
        <label htmlFor="personal-timezone" className="text-[13px] font-semibold text-fg-1">
          {t('personal_space.timezone')}
        </label>
        <select
          id="personal-timezone" value={selected} disabled={isSaving}
          onChange={(e) => setDraft(e.target.value)}
          className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none focus:border-accent">
          {timezoneChoices(space.timezone).map((zone) => (
            <option key={zone} value={zone}>{zone.replace(/_/g, ' ')}</option>
          ))}
        </select>
        <p className="text-xs text-fg-3">{t('personal_space.timezone_hint')}</p>
      </div>

      {failed && <p className="mt-3 text-[13px] text-status-red">{t('personal_space.error')}</p>}

      <div className="mt-4 flex justify-end">
        <Button type="button" style={CTA_BUTTON_STYLE} onClick={() => void handleSave()} disabled={!isDirty || isSaving}>
          {t('personal_space.submit')}
        </Button>
      </div>
    </section>
  )
}
