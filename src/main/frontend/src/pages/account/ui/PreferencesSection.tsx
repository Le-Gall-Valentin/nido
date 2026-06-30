import { Monitor, Moon, Sun } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { CTA_BUTTON_STYLE } from '@/shared/ui'
import { useTheme, type Theme, useLanguage, type Language } from '@/shared/lib'

interface OptionButtonProps {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}

function OptionButton({ active, onClick, children }: OptionButtonProps) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={`flex items-center gap-1.5 px-3 py-1.5 rounded border text-xs font-medium transition-colors ${
        active
          ? 'border-transparent'
          : 'border-border-2 bg-bg-2 text-fg-1 hover:bg-bg-3 hover:text-fg-0'
      }`}
      style={active ? CTA_BUTTON_STYLE : undefined}
    >
      {children}
    </button>
  )
}

export function PreferencesSection() {
  const { t } = useTranslation('account')
  const { theme, setTheme } = useTheme()
  const { language, setLanguage } = useLanguage()

  const themeOptions: { value: Theme; icon: React.ReactNode; labelKey: string }[] = [
    { value: 'light', icon: <Sun className="size-3.5" />, labelKey: 'preferences.theme_light' },
    { value: 'dark', icon: <Moon className="size-3.5" />, labelKey: 'preferences.theme_dark' },
    { value: 'system', icon: <Monitor className="size-3.5" />, labelKey: 'preferences.theme_system' },
  ]

  const languageOptions: { value: Language; label: string }[] = [
    { value: 'fr', label: 'FR' },
    { value: 'en', label: 'EN' },
  ]

  return (
    <section id="section-preferences" className="rounded-md border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-3.5 py-[10px] border-b border-border">
        <div className="text-xs font-semibold text-fg-0 tracking-tight">{t('preferences.title')}</div>
        <div className="text-[11px] text-fg-2 mt-px">{t('preferences.subtitle')}</div>
      </div>
      <div className="p-3.5 flex flex-col gap-3.5">
        <div className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          <span className="text-xs text-fg-1 font-medium">{t('preferences.theme_label')}</span>
          <div className="flex gap-1">
            {themeOptions.map(({ value, icon, labelKey }) => (
              <OptionButton key={value} active={theme === value} onClick={() => setTheme(value)}>
                {icon}
                {t(labelKey)}
              </OptionButton>
            ))}
          </div>
        </div>
        <div className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          <span className="text-xs text-fg-1 font-medium">{t('preferences.language_label')}</span>
          <div className="flex gap-1">
            {languageOptions.map(({ value, label }) => (
              <OptionButton key={value} active={language === value} onClick={() => setLanguage(value)}>
                {label}
              </OptionButton>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}