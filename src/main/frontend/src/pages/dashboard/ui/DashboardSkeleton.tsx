import { useTranslation } from 'react-i18next'

const BLOCK = 'rounded-2xl border border-border bg-bg-1 motion-safe:animate-pulse'
const PAIR = 'grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]'

/** The page's own shape while it loads, so nothing jumps when the blocks arrive. */
export function DashboardSkeleton() {
  const { t } = useTranslation('dashboard')
  return (
    <div role="status" aria-label={t('loading')} className="flex flex-col gap-4">
      <div className="mb-2 flex flex-col gap-2">
        <div className="h-8 w-64 max-w-full rounded-lg bg-bg-3 motion-safe:animate-pulse" />
        <div className="h-4 w-80 max-w-full rounded bg-bg-3 motion-safe:animate-pulse" />
      </div>
      <div className={PAIR}><div className={`h-72 ${BLOCK}`} /><div className={`h-72 ${BLOCK}`} /></div>
      <div className={`h-56 ${BLOCK}`} />
      <div className={PAIR}><div className={`h-80 ${BLOCK}`} /><div className={`h-80 ${BLOCK}`} /></div>
    </div>
  )
}
