import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Alert, Spinner } from '@/shared/ui'
import { settingsApi } from '../api/settingsApi'
import { SETTINGS_KEY } from '../model/fields'
import type { ISettingsApi } from '../model/ISettingsApi'
import { SettingsGroupCard } from './SettingsGroupCard'

/** The instance settings, one block per group, each saved on its own. SUPER_ADMIN only (SuperAdminRoute). */
export function AdminSettingsPage({ api = settingsApi }: { api?: ISettingsApi } = {}) {
  const { t } = useTranslation('adminSettings')
  const { data, isPending, isError } = useQuery({ queryKey: SETTINGS_KEY, queryFn: () => api.get() })

  if (isPending) return <Spinner label={t('loading')} />
  if (isError || !data) return <Alert variant="error" className="m-6">{t('load_error')}</Alert>

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-6 sm:px-6">
      <header>
        <h1 className="text-[26px] font-semibold tracking-tight text-fg-0">{t('title')}</h1>
        <p className="mt-1 text-sm text-fg-2">{t('subtitle')}</p>
      </header>
      {data.groups.map((group) => <SettingsGroupCard key={group.group} group={group} api={api} />)}
    </div>
  )
}
