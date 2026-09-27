import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { CircleCheck } from 'lucide-react'
import { resolveLocale } from '@/shared/lib'
import { useDashboardActions } from '../model/dashboardActions'
import { formatLongDay } from '../lib/dates'
import { AddMenu } from './AddMenu'

interface DashboardHeroProps {
  date: string
  attentionCount: number
  /** Whether every source answered; without it, an empty "À traiter" is not a promise that all is clear. */
  complete: boolean
  username: string
  onAddTask: () => void
}

/** The day as the page title, one sentence of digest, and "Ajouter" for those who may write. */
export function DashboardHero({ date, attentionCount, complete, username, onAddTask }: DashboardHeroProps) {
  const { t } = useTranslation('dashboard')
  const { canWrite } = useDashboardActions()
  return (
    <section className="mb-5 flex items-start justify-between gap-4 md:mb-6 md:items-end">
      <div className="min-w-0">
        <h1 className="text-[25px] font-bold leading-tight text-fg-0 md:text-[30px]">{formatLongDay(date, resolveLocale(i18next.language))}</h1>
        <p className="mt-1.5 flex items-center gap-1.5 text-sm text-fg-2 md:text-[15px]">
          {attentionCount > 0
            ? t('hero.digest', { count: attentionCount, name: username })
            : complete
              ? <><CircleCheck className="size-4 shrink-0 text-accent" aria-hidden="true" />{t('hero.all_clear', { name: username })}</>
              : t('hero.partial', { name: username })}
        </p>
      </div>
      {canWrite && <AddMenu onAddTask={onAddTask} />}
    </section>
  )
}
