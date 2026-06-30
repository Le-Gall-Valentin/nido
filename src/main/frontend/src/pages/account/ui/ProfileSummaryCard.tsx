import { useTranslation } from 'react-i18next'
import type { User } from '@/entities/user'
import { RolePill, UserAvatar } from '@/entities/user'

interface ProfileSummaryCardProps {
  user: User
}

export function ProfileSummaryCard({ user }: ProfileSummaryCardProps) {
  const { t, i18n } = useTranslation(['account', 'shell'])

  const createdDate = new Date(user.createdAt).toLocaleDateString(
    i18n.language === 'fr' ? 'fr-FR' : 'en-GB',
    { day: '2-digit', month: 'long', year: 'numeric' }
  )

  return (
    <div className="rounded-md border border-border bg-bg-1 p-3.5 mb-4">
      <div className="flex items-center gap-4">
        <UserAvatar
          username={user.username}
          role={user.role}
          className="w-[52px] h-[52px] rounded-[10px] text-[18px]"
        />
        <div className="flex-1 min-w-0">
          <div className="text-[18px] font-semibold text-fg-0 tracking-[-0.005em] truncate leading-snug">
            {user.username}
          </div>
          <div className="flex flex-wrap items-center gap-x-[10px] gap-y-1 mt-1.5">
            <RolePill role={user.role} label={t(`user.role.${user.role}`, { ns: 'shell' })} />
            <span className="font-mono text-[11px] text-fg-3 truncate">{user.email}</span>
            <span className="font-mono text-[11px] text-fg-3">
              {t('member_since', { date: createdDate })}
            </span>
          </div>
        </div>
      </div>
    </div>
  )
}
