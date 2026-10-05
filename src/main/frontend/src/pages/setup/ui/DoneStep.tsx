import { useTranslation } from 'react-i18next'
import { LEAD_CLASS, TITLE_CLASS } from './styles'

/** Set up from an address that is not the public one: signing in here would set a cookie the public address never sees. */
export function DoneStep({ url }: { url: string }) {
  const { t } = useTranslation('setup')
  return (
    <div>
      <h1 className={TITLE_CLASS}>{t('done.title')}</h1>
      <p className={LEAD_CLASS}>{t('done.lead')}</p>
      <a href={url} className="break-all text-[15px] font-semibold text-accent hover:underline">{url}</a>
    </div>
  )
}
