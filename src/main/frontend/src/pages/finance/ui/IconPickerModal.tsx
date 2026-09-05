import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { resolveCategoryIcon } from '../lib/resolveCategoryIcon'
import { ALL_ICON_NAMES } from '../lib/lucideIconNames'

const DEFAULT_VISIBLE_COUNT = 96

interface IconPickerModalProps {
  initialIcon: string
  initialColor: string
  onConfirm: (icon: string, color: string) => void
  onCancel: () => void
  /** Overridable for tests — defaults to every lucide-react icon. */
  iconNames?: string[]
}

export function IconPickerModal({ initialIcon, initialColor, onConfirm, onCancel, iconNames = ALL_ICON_NAMES }: IconPickerModalProps) {
  const { t } = useTranslation('finance')
  const [selectedIcon, setSelectedIcon] = useState(initialIcon)
  const [color, setColor] = useState(initialColor)
  const [query, setQuery] = useState('')

  const trimmed = query.trim().toLowerCase()
  const visibleIcons = trimmed ? iconNames.filter((name) => name.toLowerCase().includes(trimmed)) : iconNames.slice(0, DEFAULT_VISIBLE_COUNT)
  const PreviewIcon = resolveCategoryIcon(selectedIcon)

  return (
    <Dialog open onClose={onCancel} title={t('categories.appearance_title')} maxWidth="max-w-lg">
      <h3 className="mb-4 text-[19px] font-semibold text-fg-0">{t('categories.appearance_title')}</h3>

      <div className="mb-4 flex items-center gap-3">
        <div className="grid size-11 shrink-0 place-items-center rounded-[10px] text-white" style={{ backgroundColor: color }}>
          <PreviewIcon size={22} />
        </div>
        <input type="color" value={color} onChange={(e) => setColor(e.target.value)}
          aria-label={t('categories.color_label')}
          className="size-11 shrink-0 cursor-pointer rounded-[10px] border-[1.5px] border-border bg-bg-1 p-1" />
        <div className="flex-1">
          <Input label={t('categories.icon_search_label')} srOnlyLabel placeholder={t('categories.icon_search_placeholder')}
            value={query} onChange={(e) => setQuery(e.target.value)} />
        </div>
      </div>

      <div className="grid max-h-72 grid-cols-8 gap-1 overflow-y-auto rounded-[10px] bg-bg-2 p-2">
        {visibleIcons.map((name) => {
          const Icon = resolveCategoryIcon(name)
          const isSelected = name === selectedIcon
          return (
            <button key={name} type="button" aria-label={name} aria-pressed={isSelected} onClick={() => setSelectedIcon(name)}
              className={`grid aspect-square place-items-center rounded-[8px] ${isSelected ? 'bg-accent text-white' : 'text-fg-2 hover:bg-bg-1'}`}>
              <Icon size={18} />
            </button>
          )
        })}
        {visibleIcons.length === 0 && (
          <p className="col-span-8 py-4 text-center text-sm text-fg-3">{t('categories.icon_no_results')}</p>
        )}
      </div>

      <div className="mt-4 flex justify-end gap-2">
        <Button type="button" onClick={onCancel}>{t('form.cancel')}</Button>
        <Button type="button" onClick={() => onConfirm(selectedIcon, color)} style={CTA_BUTTON_STYLE}>{t('categories.icon_confirm')}</Button>
      </div>
    </Dialog>
  )
}
