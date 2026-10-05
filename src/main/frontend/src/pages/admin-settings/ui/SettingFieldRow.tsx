import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { useSettingWording } from '@/entities/instance-settings'
import { Input, PasswordInput, Switch } from '@/shared/ui'
import { FIELD_KINDS } from '../model/fields'
import type { SettingField } from '../model/types'
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
  const labelId = useId()
  const kind = FIELD_KINDS[field.key] ?? { kind: 'text' }
  const locked = field.source === 'ENVIRONMENT'
  const label = t(`field.${field.key}`)

  let control
  if (kind.kind === 'switch') {
    control = (
      <div className="flex items-center justify-between gap-3">
        <span id={labelId} className="text-[13px] font-semibold text-fg-1">{label}</span>
        <Switch checked={value === 'true'} disabled={locked || busy} onChange={(checked) => onChange(String(checked))} aria-labelledby={labelId} />
      </div>
    )
  } else if (kind.kind === 'select') {
    control = (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={labelId} className="text-[13px] font-semibold text-fg-1">{label}</label>
        <select
          id={labelId}
          value={value}
          disabled={locked}
          onChange={(event) => onChange(event.target.value)}
          className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-[14.5px] text-fg-0 disabled:opacity-60"
        >
          {kind.options.map((option) => <option key={option} value={option}>{t(`option.${option}`)}</option>)}
        </select>
      </div>
    )
  } else if (kind.kind === 'password') {
    control = (
      <PasswordInput
        label={label}
        id={labelId}
        value={value}
        disabled={locked}
        placeholder={field.set ? t('secret.set') : ''}
        autoComplete="new-password"
        onChange={(event) => onChange(event.target.value)}
      />
    )
  } else {
    control = (
      <Input
        label={label}
        id={labelId}
        value={value}
        disabled={locked}
        inputMode={kind.kind === 'number' ? 'numeric' : undefined}
        // Next to a password field, a password manager takes any text field for the user's own sign-in.
        autoComplete="off"
        spellCheck={false}
        title={locked ? t('source.environment_hint', { variable: field.variable }) : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
    )
  }

  return (
    <div>
      <div className="mb-1 flex justify-end empty:hidden"><SourceBadge source={field.source} variable={field.variable} /></div>
      {control}
      {problem && <p className="mt-1 text-[12.5px] text-status-red">{wording.problem(problem)}</p>}
      {!locked && !field.required && field.source === 'DATABASE' && (
        <button type="button" disabled={busy} onClick={onReset} className="mt-1 text-[12.5px] font-semibold text-accent hover:underline disabled:opacity-50">
          {field.secret ? t('secret.clear') : t('action.reset')}
        </button>
      )}
    </div>
  )
}
