import { useTranslation } from 'react-i18next'
import { Alert } from '@/shared/ui'
import type { GroupExtrasProps } from './types'

/** Plain http travels in clear; an https address saved from an http page ends this session. */
export function PublicUrlNotes({ state }: GroupExtrasProps) {
  const { t } = useTranslation('adminSettings')
  const publicUrl = (state.values['public-url'] ?? '').toLowerCase()
  return (
    <>
      {publicUrl.startsWith('http://') && <Alert variant="warning" className="mt-4">{t('public_url.http_warning')}</Alert>}
      {publicUrl.startsWith('https://') && window.location.protocol === 'http:' && (
        <Alert variant="warning" className="mt-4">{t('public_url.https_from_http', { url: state.values['public-url'] })}</Alert>
      )}
    </>
  )
}
