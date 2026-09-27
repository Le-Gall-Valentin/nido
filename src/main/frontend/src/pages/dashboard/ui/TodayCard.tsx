import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { Calendar, CalendarDays, Sunrise } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { resolveLocale } from '@/shared/lib'
import type { AgendaCard, AgendaEvent } from '@/entities/dashboard'
import { useDashboardActions } from '../model/dashboardActions'
import type { ZonedNow } from '../lib/useNow'
import { addDaysIso, formatDayMonth, shortTime } from '../lib/dates'
import { isEventOver, nowLineIndex } from '../lib/nowLine'
import { eventBarClass, eventTintClass } from '../lib/eventColor'
import { DashboardCard } from './DashboardCard'
import { CardList, CardRow, RowLead } from './CardRow'
import { CardGroup } from './CardGroup'
import { AvatarStack } from './AvatarStack'
import { TaskRow } from './TaskRow'

function AllDayBanner({ event, date }: { event: AgendaEvent; date: string }) {
  const { t } = useTranslation('dashboard')
  const locale = resolveLocale(i18next.language)
  return (
    <div className={`mb-1 flex items-center gap-2 rounded-[10px] px-3 py-2 text-[13.5px] font-semibold ${eventTintClass(event.color)}`}>
      <CalendarDays className="size-4 shrink-0" aria-hidden="true" />
      <span className="min-w-0 truncate">{event.title}</span>
      <span className="ml-auto shrink-0 text-[12.5px] font-medium opacity-80">
        {event.endDate > date
          ? t('today.until_day', { date: formatDayMonth(event.endDate, locale) })
          // A timed event that began on an earlier day (a night shift) ends at a time today, not "all day".
          : event.endTime ? t('today.until', { time: shortTime(event.endTime) }) : t('today.all_day')}
      </span>
    </div>
  )
}

/** The one memorable element of the page: where the day stands, right now. */
function NowLine({ time }: { time: string }) {
  const { t } = useTranslation('dashboard')
  return (
    <li aria-label={t('today.now', { time })} className="flex min-h-[22px] items-center gap-3">
      <span className="w-[50px] shrink-0 text-[13px] font-bold tabular-nums text-accent">{time}</span>
      <span aria-hidden="true"
        className="relative h-0.5 flex-1 rounded bg-accent before:absolute before:-left-1 before:-top-[3px] before:size-2 before:rounded-full before:bg-accent" />
    </li>
  )
}

/** A timed event that starts today, wherever it ends: tonight, tomorrow night, or days later. */
function EventRow({ event, date, over, divider }: { event: AgendaEvent; date: string; over: boolean; divider: boolean }) {
  const { t } = useTranslation('dashboard')
  let meta: ReactNode
  if (event.location) meta = <span>{event.location}</span>
  else if (event.endDate > addDaysIso(date, 1)) {
    meta = <span>{t('today.until_day', { date: formatDayMonth(event.endDate, resolveLocale(i18next.language)) })}</span>
  } else if (event.endTime) {
    // An evening past midnight ends "at 01:00 tomorrow", not at an 01:00 that would come before it started.
    meta = <span>{t(event.endDate > date ? 'today.until_tomorrow' : 'today.until', { time: shortTime(event.endTime) })}</span>
  }
  return (
    <CardRow divider={divider} muted={over}
      lead={<>
        <RowLead>{event.startTime ? shortTime(event.startTime) : ''}</RowLead>
        <span aria-hidden="true" className={`w-[3px] self-stretch rounded-full ${eventBarClass(event.color)}`} />
      </>}
      title={event.title}
      meta={meta}
      trail={event.participantIds.length > 0 ? <AvatarStack memberIds={event.participantIds} /> : undefined} />
  )
}

interface TodayCardProps {
  card: AgendaCard
  /** The dashboard's date, the space's today when it was computed. */
  date: string
  now: ZonedNow
}

/**
 * Today on the household's calendar: all-day events first, then the timed ones with the now line and
 * what is over dimmed, today's to-dos, and a glance at tomorrow. A page left open past midnight shows
 * yesterday's list without a now line until it refetches, rather than a timeline half greyed out.
 */
export function TodayCard({ card, date, now }: TodayCardProps) {
  const { t } = useTranslation('dashboard')
  const { spaceId } = useDashboardActions()
  const isToday = now.date === date
  const lineAt = isToday && card.timed.length > 0 ? nowLineIndex(card.timed, now.time) : -1
  const empty = card.allDay.length === 0 && card.timed.length === 0 && card.dueToday.length === 0

  const rows: ReactNode[] = []
  card.timed.forEach((event, index) => {
    if (index === lineAt) rows.push(<NowLine key="now" time={now.time} />)
    rows.push(<EventRow key={event.id} event={event} date={date} over={isToday && isEventOver(event, date, now.time)}
      divider={index !== 0 && index !== lineAt} />)
  })
  if (lineAt === card.timed.length && lineAt > 0) rows.push(<NowLine key="now" time={now.time} />)

  const tomorrow = card.tomorrow
  const footer = tomorrow ? (
    <p className="flex min-w-0 items-center gap-2 text-[13px] text-fg-3">
      <Sunrise className="size-4 shrink-0" aria-hidden="true" />
      {t('today.tomorrow')}
      <b className="truncate font-semibold text-fg-1">
        {tomorrow.startTime ? t('today.tomorrow_at', { title: tomorrow.title, time: shortTime(tomorrow.startTime) }) : tomorrow.title}
      </b>
    </p>
  ) : undefined

  return (
    <DashboardCard icon={Calendar} title={t('today.title')}
      link={{ to: ROUTES.spaceOrganisationCalendar(spaceId), label: t('today.link') }} footer={footer}>
      {card.allDay.map((event) => <AllDayBanner key={event.id} event={event} date={date} />)}
      {rows.length > 0 && <CardList>{rows}</CardList>}
      {card.dueToday.length > 0 && (
        <CardGroup title={t('today.due_today')}>
          {card.dueToday.map((task) => <TaskRow key={task.id} task={task} dateStyle="none" />)}
        </CardGroup>
      )}
      {empty && <p className="py-3 text-sm text-fg-3">{t('today.empty')}</p>}
    </DashboardCard>
  )
}
