import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useCopy } from '@/shared/lib'
import type { CreatedSubscriptionLink } from '@/entities/calendar'

function AddressField({ title, value }: { title: string; value: string }) {
  const { t } = useTranslation('calendarSync')
  const { copy, copied, failed } = useCopy()
  return (
    <div className="flex flex-col gap-1.5">
      <label className="flex flex-col gap-1.5">
        <span className="text-[13px] font-semibold text-fg-1">{title}</span>
        <input readOnly value={value} aria-label={title}
          onFocus={(e) => e.currentTarget.select()}
          className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-2 font-mono text-xs text-fg-0" />
      </label>
      <div className="flex flex-wrap gap-2">
        <Button type="button" aria-label={`${t('subscribe.copy')} — ${title}`} onClick={() => void copy(value)}>
          {copied ? t('subscribe.copied') : t('subscribe.copy')}
        </Button>
      </div>
      {failed && <p role="alert" className="text-sm text-status-red">{t('subscribe.copy_failed')}</p>}
    </div>
  )
}

/** The address, the one time it exists outside the calendar app given it: copy it, open it, or lose it. */
export function CreatedLinkNotice({ created, onDone }: { created: CreatedSubscriptionLink; onDone: () => void }) {
  const { t } = useTranslation('calendarSync')
  return (
    <div className="flex flex-col gap-3">
      <AddressField title={t('subscribe.created_title')} value={created.url} />
      <AddressField title={t('subscribe.webcal_title')} value={created.webcalUrl} />
      <div className="flex flex-wrap gap-2">
        <a href={created.webcalUrl}
          className="inline-flex items-center rounded-[10px] border border-border px-4 py-2 text-sm font-semibold text-fg-1 hover:bg-bg-2">
          {t('subscribe.open')}
        </a>
      </div>
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
