import { Monitor, Moon, Sun } from 'lucide-react'
import { useTranslation } from 'react-i18next'
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
      className={`flex items-center gap-1.5 px-[15px] py-[9px] rounded-[9px] border-[1.5px] text-[13.5px] font-semibold transition-colors ${
        active
          ? 'border-accent bg-accent-dim text-status-green'
          : 'border-border bg-bg-1 text-fg-2 hover:text-fg-0'
      }`}
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
    { value: 'fr', label: 'Français' },
    { value: 'en', label: 'English' },
  ]

  return (
    <section id="section-preferences" className="rounded-2xl border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-7 pt-6">
        <h3 className="text-lg font-semibold text-fg-0">{t('preferences.title')}</h3>
        <p className="text-[13.5px] text-fg-2 mt-0.5">{t('preferences.subtitle')}</p>
      </div>
      <div className="px-7 py-5 flex flex-col gap-[22px]">
        <div className="flex flex-col gap-2.5">
          <span className="text-sm text-fg-0 font-semibold">{t('preferences.theme_label')}</span>
          <div className="flex gap-[9px] flex-wrap">
            {themeOptions.map(({ value, icon, labelKey }) => (
              <OptionButton key={value} active={theme === value} onClick={() => setTheme(value)}>
                {icon}
                {t(labelKey)}
              </OptionButton>
            ))}
          </div>
        </div>
        <div className="h-px bg-bg-3 -my-[5px]" aria-hidden="true" />
        <div className="flex flex-col gap-2.5">
          <span className="text-sm text-fg-0 font-semibold">{t('preferences.language_label')}</span>
          <div className="flex gap-[9px] flex-wrap">
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