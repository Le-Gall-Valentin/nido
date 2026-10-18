import { useTranslation } from 'react-i18next'
import type { ExportableSource } from '@/entities/calendar'

const SOURCES: ExportableSource[] = ['EVENT', 'TASK', 'FINANCE', 'MEAL', 'SAVINGS']

/** The sources an export or an address writes, in the calendar's own order. */
export function SourceChecklist({ selected, onChange }: { selected: ExportableSource[]; onChange: (next: ExportableSource[]) => void }) {
  const { t } = useTranslation('calendarSync')
  const toggle = (source: ExportableSource) => onChange(selected.includes(source)
    ? selected.filter((s) => s !== source)
    : SOURCES.filter((s) => s === source || selected.includes(s)))
  return (
    <fieldset className="flex flex-col gap-1.5">
      <legend className="mb-1 text-[13px] font-semibold text-fg-1">{t('sources.label')}</legend>
      {SOURCES.map((source) => (
        <label key={source} className="flex items-center gap-2 text-sm text-fg-1">
          <input type="checkbox" checked={selected.includes(source)} onChange={() => toggle(source)} />
          {t(`sources.${source}`)}
        </label>
      ))}
    </fieldset>
  )
}

export function OnlyMineToggle({ checked, onChange }: { checked: boolean; onChange: (next: boolean) => void }) {
  const { t } = useTranslation('calendarSync')
  return (
    <label className="flex items-start gap-2 text-sm text-fg-1">
      <input type="checkbox" className="mt-0.5" aria-label={t('only_mine')} checked={checked} onChange={(e) => onChange(e.target.checked)} />
      <span>
        {t('only_mine')}
        <span className="block text-xs text-fg-3">{t('only_mine_hint')}</span>
      </span>
    </label>
  )
}
