import { useTranslation } from 'react-i18next'
import { NetworkArtwork } from './NetworkArtwork'

const BRAND_PANEL_BG =
  'radial-gradient(900px 600px at 20% 20%, var(--brand-accent-glow-1), transparent 60%), radial-gradient(700px 600px at 80% 80%, var(--brand-accent-glow-2), transparent 60%), linear-gradient(145deg, var(--brand-panel-from) 0%, var(--brand-panel-to) 100%)'

const GRID_BG_IMAGE =
  'linear-gradient(var(--brand-grid-line) 1px, transparent 1px), linear-gradient(90deg, var(--brand-grid-line) 1px, transparent 1px)'

const GRID_MASK =
  'radial-gradient(ellipse 80% 60% at 50% 50%, #000 30%, transparent 80%)'

const BRAND_ICON_BG = 'linear-gradient(135deg, var(--brand-icon-from), var(--brand-icon-to))'

const BRAND_ICON_SHADOW =
  '0 1px 0 rgba(255,255,255,0.15) inset, 0 2px 6px var(--brand-icon-glow)'

const HERO_TEXT_BG = 'linear-gradient(135deg, var(--brand-hero-text-from) 0%, var(--brand-hero-text-to) 100%)'

const QUOTE_BG = 'var(--brand-quote-bg)'

export function LoginBrandPanel() {
  const { t } = useTranslation('login')

  return (
    <div
      aria-hidden="true"
      className="relative hidden flex-col overflow-hidden border-r border-border md:flex"
      style={{ background: BRAND_PANEL_BG }}
    >
      {/* Grille décorative */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: GRID_BG_IMAGE,
          backgroundSize: '32px 32px',
          maskImage: GRID_MASK,
          WebkitMaskImage: GRID_MASK,
        }}
      />

      <div className="relative mx-auto flex h-full w-full max-w-[560px] flex-col px-11 py-10">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div
            className="grid size-9 shrink-0 place-items-center rounded-xl font-mono text-base font-bold text-bg-0"
            style={{ background: BRAND_ICON_BG, boxShadow: BRAND_ICON_SHADOW }}
          >
            S
          </div>
          <span className="text-[17px] font-semibold tracking-tight text-fg-0">
            {t('brand')}
          </span>
        </div>

        {/* Hero */}
        <div className="mt-16">
          <p
            className="mb-4 text-[26px] lg:text-[34px] font-semibold leading-[1.1] tracking-tight"
            style={{
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
          <p className="max-w-[420px] text-[15px] leading-relaxed text-fg-1">
            {t('hero.description')}
          </p>
        </div>

        {/* SVG réseau animé */}
        <div className="mx-auto my-10 w-full max-w-[360px]">
          <NetworkArtwork />
        </div>

        {/* Citation */}
        <div
          className="mt-auto grid max-w-[440px] grid-cols-[28px_1fr] gap-3 rounded-xl border border-border p-4"
          style={{ background: QUOTE_BG }}
        >
          <span
            className="text-4xl leading-none text-accent"
            style={{ fontFamily: 'Georgia, serif', marginTop: '4px' }}
          >
            "
          </span>
          <p className="text-[13px] italic leading-relaxed text-fg-1">
            {t('quote')}
          </p>
        </div>
      </div>
    </div>
  )
}