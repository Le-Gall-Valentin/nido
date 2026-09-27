import { UserAvatar } from '@/entities/user'
import { useDashboardActions } from '../model/dashboardActions'

/** Who a task is for, who comes to an event: three avatars at most, then "+n". */
export function AvatarStack({ memberIds, max = 3 }: { memberIds: string[]; max?: number }) {
  const { memberName } = useDashboardActions()
  const shown = memberIds.slice(0, max)
  const hidden = memberIds.length - shown.length
  return (
    <span role="img" aria-label={memberIds.map(memberName).join(', ')} className="flex -space-x-1.5">
      {shown.map((id) => (
        <UserAvatar key={id} username={memberName(id)} role="USER" className="size-6 rounded-full border-2 border-bg-1 text-[10px]" />
      ))}
      {hidden > 0 && (
        <span className="grid size-6 place-items-center rounded-full border-2 border-bg-1 bg-bg-3 text-[10px] font-bold text-fg-2">+{hidden}</span>
      )}
    </span>
  )
}
