import { useTranslation } from 'react-i18next'
import { SettingInput, useSettingWording, type SettingField } from '@/entities/instance-settings'
import { SourceBadge } from './SourceBadge'

interface Props {
  field: SettingField
  value: string
  problem?: string
  busy: boolean
  onChange: (value: string) => void
  onReset: () => void
}

export function SettingFieldRow({ field, value, problem, busy, onChange, onReset }: Props) {
  const { t } = useTranslation('adminSettings')
  const wording = useSettingWording()
  const locked = field.source === 'ENVIRONMENT'
  return (
    <div>
      <div className="mb-1 flex justify-end empty:hidden"><SourceBadge source={field.source} variable={field.variable} /></div>
      <SettingInput
        settingKey={field.key}
        value={value}
        onChange={onChange}
        disabled={locked}
        placeholder={field.secret && field.set ? t('secret.set') : undefined}
        title={locked ? t('source.environment_hint', { variable: field.variable }) : undefined}
      />
      {problem && <p className="mt-1 text-[12.5px] text-status-red">{wording.problem(problem)}</p>}
      {!locked && !field.required && field.source === 'DATABASE' && (
        <button type="button" disabled={busy} onClick={onReset} className="mt-1 text-[12.5px] font-semibold text-accent hover:underline disabled:opacity-50">
          {field.secret ? t('secret.clear') : t('action.reset')}
        </button>
      )}
    </div>
  )
}
