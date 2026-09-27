import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { BellRing, CalendarX2, HandCoins, Mail, TriangleAlert } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { formatAmount, formatRelativeTime } from '@/shared/lib'
import { useAcceptInvitation } from '@/entities/space'
import type { AttentionItem } from '@/entities/dashboard'
import { useDashboardActions } from '../model/dashboardActions'
import { DashboardCard } from './DashboardCard'
import { CardRow } from './CardRow'
import { Counter, SmallButton, SmallLink, Tint } from './cardParts'

function itemKey(item: AttentionItem): string {
  switch (item.kind) {
    case 'OVERDUE_TASKS': return item.kind
    case 'BUDGET_OVERRUN': return `${item.kind}:${item.categoryId}`
    case 'DEBT': return `${item.kind}:${item.toMemberId}`
    case 'INVITATION': return `${item.kind}:${item.invitationId}`
  }
}

function AttentionRow({ item }: { item: AttentionItem }) {
  const { t } = useTranslation('dashboard')
  const { spaceId, canWrite, memberName, settle, reportError } = useDashboardActions()
  const accept = useAcceptInvitation()

  switch (item.kind) {
    case 'OVERDUE_TASKS':
      return (
        <CardRow lead={<Tint icon={CalendarX2} tone="red" />}
          title={t('attention.overdue_tasks', { count: item.count })}
          meta={<span className="truncate">{item.titles.join(', ')}</span>}
          trail={<SmallLink to={ROUTES.spaceOrganisationTasks(spaceId)}>{t('attention.see')}</SmallLink>} />
      )
    case 'BUDGET_OVERRUN':
      return (
        <CardRow lead={<Tint icon={TriangleAlert} tone="red" />}
          title={t('attention.budget_overrun', { label: item.label })}
          meta={<span>{t('attention.budget_overrun_detail', { spent: formatAmount(item.spent), limit: formatAmount(item.limit) })}</span>}
          trail={<SmallLink to={ROUTES.spaceFinance(spaceId)}>{t('attention.see')}</SmallLink>} />
      )
    case 'DEBT':
      return (
        <CardRow lead={<Tint icon={HandCoins} tone="orange" />}
          title={t('attention.debt', { amount: formatAmount(item.amount), name: memberName(item.toMemberId) })}
          meta={<span>{t('attention.debt_detail')}</span>}
          trail={canWrite
            ? <SmallButton onClick={() => settle({ toMemberId: item.toMemberId, amount: item.amount })}>{t('attention.settle')}</SmallButton>
            : undefined} />
      )
    case 'INVITATION': {
      const role = t(`role.${item.role}`)
      const expires = formatRelativeTime(item.expiresAt, i18next.language)
      return (
        <CardRow lead={<Tint icon={Mail} tone="blue" />}
          title={t('attention.invitation', { space: `${item.spaceGlyph} ${item.spaceName}` })}
          meta={<span>{item.invitedByUsername
            ? t('attention.invitation_from', { name: item.invitedByUsername, role, expires })
            : t('attention.invitation_detail', { role, expires })}</span>}
          // Accepting is the caller's own business, not a write in this space: a viewer here may accept too.
          trail={<SmallButton primary disabled={accept.isPending}
            onClick={() => accept.mutate(item.invitationId, { onError: reportError })}>{t('attention.accept')}</SmallButton>} />
      )
    }
  }
}

/**
 * Everything that needs an action, whatever module it comes from, already sorted by the server
 * (urgent first). Two columns when the card is wide, one on a phone.
 */
export function AttentionCard({ items }: { items: AttentionItem[] }) {
  const { t } = useTranslation('dashboard')
  return (
    <DashboardCard icon={BellRing} tone="attention" title={t('attention.title')} aside={<Counter tone="danger">{items.length}</Counter>}>
      <ul className="m-0 grid list-none grid-cols-1 gap-x-8 p-0 @2xl:grid-cols-2 @2xl:[&>li:nth-child(2)]:border-t-0">
        {items.map((item) => <AttentionRow key={itemKey(item)} item={item} />)}
      </ul>
    </DashboardCard>
  )
}
