import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, PasswordInput, Switch } from '@/shared/ui'
import { FIELD_KINDS } from '../model/fieldKinds'

interface Props {
  /** The setting's code, `mail.host`…: it decides the label and the kind of control. */
  settingKey: string
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  placeholder?: string
  title?: string
}

/**
 * One setting typed in, the same on the setup screen and on the settings page: its label, and the
 * control its kind calls for. Nothing autofills it — next to a password field, a password manager takes
 * any text field for the user's own sign-in.
 */
export function SettingInput({ settingKey, value, onChange, disabled = false, placeholder, title }: Props) {
  const { t } = useTranslation('instanceSettings')
  const id = useId()
  const kind = FIELD_KINDS[settingKey] ?? { kind: 'text' }
  const label = t(`field.${settingKey}`)

  if (kind.kind === 'switch') {
    return (
      <div className="flex items-center justify-between gap-3">
        <span id={id} className="text-[13px] font-semibold text-fg-1">{label}</span>
        <Switch checked={value === 'true'} disabled={disabled} onChange={(checked) => onChange(String(checked))} aria-labelledby={id} />
      </div>
    )
  }
  if (kind.kind === 'select') {
    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={id} className="text-[13px] font-semibold text-fg-1">{label}</label>
        <select
          id={id}
          name={settingKey}
          value={value}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value)}
          className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-[14.5px] text-fg-0 disabled:opacity-60"
        >
          {kind.options.map((option) => <option key={option} value={option}>{t(`option.${option}`)}</option>)}
        </select>
      </div>
    )
  }
  if (kind.kind === 'password') {
    return (
      <PasswordInput
        label={label}
        id={id}
        name={settingKey}
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        autoComplete="new-password"
        onChange={(event) => onChange(event.target.value)}
      />
    )
  }
  return (
    <Input
      label={label}
      id={id}
      name={settingKey}
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      inputMode={kind.kind === 'number' ? 'numeric' : undefined}
      autoComplete="off"
      spellCheck={false}
      title={title}
      onChange={(event) => onChange(event.target.value)}
    />
  )
}
