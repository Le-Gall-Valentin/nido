import { UserAvatarStack } from '@/entities/user'
import { useDashboardActions } from '../model/dashboardActions'

/**
 * Who a task is for, who comes to an event: three avatars at most, then "+n". A dashboard row opens
 * nothing, so a tap on the avatars names everyone, as a hover does.
 */
export function AvatarStack({ memberIds, max = 3 }: { memberIds: string[]; max?: number }) {
  const { memberName } = useDashboardActions()
  return (
    <UserAvatarStack people={memberIds.map((id) => ({ userId: id, name: memberName(id) }))} max={max} standalone
      className="rounded-full" />
  )
}
