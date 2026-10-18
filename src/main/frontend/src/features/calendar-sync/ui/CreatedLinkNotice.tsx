import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useCopy } from '@/shared/lib'
import type { CreatedSubscriptionLink } from '@/entities/calendar'

/** The address, the one time it exists outside the calendar app given it: copy it, open it, or lose it. */
export function CreatedLinkNotice({ created, onDone }: { created: CreatedSubscriptionLink; onDone: () => void }) {
  const { t } = useTranslation('calendarSync')
  const { copy, copied, failed } = useCopy()
  return (
    <div className="flex flex-col gap-3">
      <label className="flex flex-col gap-1.5">
        <span className="text-[13px] font-semibold text-fg-1">{t('subscribe.created_title')}</span>
        <input readOnly value={created.url} aria-label={t('subscribe.created_title')}
          onFocus={(e) => e.currentTarget.select()}
          className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-2 font-mono text-xs text-fg-0" />
      </label>
      <div className="flex flex-wrap gap-2">
        <Button type="button" onClick={() => void copy(created.url)}>{copied ? t('subscribe.copied') : t('subscribe.copy')}</Button>
        <a href={created.webcalUrl}
          className="inline-flex items-center rounded-[10px] border border-border px-4 py-2 text-sm font-semibold text-fg-1 hover:bg-bg-2">
          {t('subscribe.open')}
        </a>
      </div>
      {failed && <p role="alert" className="text-sm text-status-red">{t('subscribe.copy_failed')}</p>}
      <p className="rounded-lg bg-status-orange-dim px-3 py-2 text-xs text-status-orange">{t('subscribe.created_warning')}</p>
      <ul className="flex list-disc flex-col gap-1 pl-4 text-xs text-fg-2">
        <li>{t('subscribe.help_google')}</li>
        <li>{t('subscribe.help_apple')}</li>
        <li>{t('subscribe.help_outlook')}</li>
      </ul>
      <div className="flex justify-end">
        <Button type="button" style={CTA_BUTTON_STYLE} onClick={onDone}>{t('subscribe.done')}</Button>
      </div>
    </div>
  )
}
