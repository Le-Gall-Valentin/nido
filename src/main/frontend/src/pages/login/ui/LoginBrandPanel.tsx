import { useTranslation } from 'react-i18next'
import { Lock } from 'lucide-react'
import { NidoMark } from '@/shared/ui'

const BRAND_PANEL_BG =
  'linear-gradient(160deg, var(--brand-panel-from) 0%, var(--brand-panel-to) 100%)'

const HERO_TEXT_BG = 'linear-gradient(135deg, var(--brand-hero-text-from) 0%, var(--brand-hero-text-to) 100%)'

export function LoginBrandPanel() {
  const { t } = useTranslation('login')

  return (
    <div
      aria-hidden="true"
      className="relative hidden flex-col overflow-hidden md:flex"
      style={{ background: BRAND_PANEL_BG }}
    >
      {/* Cercles décoratifs */}
      <div
        className="pointer-events-none absolute -bottom-20 -right-20 size-80 rounded-full"
        style={{ background: 'var(--brand-accent-glow-1)' }}
      />
      <div
        className="pointer-events-none absolute right-10 top-20 size-36 rounded-full"
        style={{ background: 'var(--brand-accent-glow-2)' }}
      />

      <div className="relative mx-auto flex h-full w-full max-w-[560px] flex-col justify-between px-11 py-10">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div
            className="grid size-10 shrink-0 place-items-center rounded-xl"
            style={{ background: 'var(--brand-panel-title)', color: 'var(--brand-panel-from)' }}
          >
            <NidoMark size={22} />
          </div>
          <span
            className="text-2xl font-bold tracking-tight"
            style={{ fontFamily: 'var(--font-family-display)', color: 'var(--brand-panel-title)' }}
          >
            {t('brand')}
          </span>
        </div>

        {/* Hero */}
        <div className="max-w-[420px]">
          <p
            className="mb-4 text-[26px] lg:text-[34px] font-semibold leading-[1.1] tracking-tight"
            style={{
              fontFamily: 'var(--font-family-display)',
              background: HERO_TEXT_BG,
              WebkitBackgroundClip: 'text',
              backgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            {t('hero.headline_1')}
            <br />
            {t('hero.headline_2')}
          </p>
          <p className="text-[15px] leading-relaxed" style={{ color: 'var(--brand-panel-desc)' }}>
            {t('hero.description')}
          </p>
        </div>

        {/* Réassurance */}
        <div className="flex items-center gap-7 text-sm" style={{ color: 'var(--brand-panel-desc)' }}>
          <span className="flex items-center gap-1.5">
            <Lock size={14} />
            {t('panel.encrypted')}
          </span>
          <span>{t('panel.twofa')}</span>
        </div>
      </div>
    </div>
  )
}
