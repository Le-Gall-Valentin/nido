import i18next from 'i18next'
import { resolveLocale } from '@/shared/lib'
import { Tooltip } from '@/shared/ui'
import { UserAvatar } from './UserAvatar'

export interface AvatarPerson {
  userId: string
  /** What the bubble says: the screen's own name for them, a former member's included. */
  name: string
}

interface UserAvatarStackProps {
  people: AvatarPerson[]
  /** How many avatars before "+n". */
  max?: number
  /** Alone on screen: a tap shows the names too. Inside a row that opens a detail, only a mouse does. */
  standalone?: boolean
  /** Size, shape and outline of each avatar, and of the "+n" after them. */
  avatarClassName?: string
  className?: string
}

/**
 * Who a task is for, who comes to an event, who saved towards a goal: a few overlapping avatars, then
 * "+n". Overlapping 24px circles are too small to tap one by one, so the bubble names everyone at once,
 * the ones behind "+n" included.
 */
export function UserAvatarStack({
  people, max = 3, standalone = false, avatarClassName = 'size-6 rounded-full border-2 border-bg-1 text-[10px]', className = '',
}: UserAvatarStackProps) {
  const shown = people.slice(0, max)
  const hidden = people.length - shown.length
  const names = new Intl.ListFormat(resolveLocale(i18next.language), { type: 'conjunction' }).format(people.map((p) => p.name))
  return (
    <Tooltip label={names} standalone={standalone} className={`flex shrink-0 -space-x-1.5 ${className}`}>
      {shown.map((person) => (
        <UserAvatar key={person.userId} userId={person.userId} username={person.name} className={avatarClassName} />
      ))}
      {hidden > 0 && (
        <span aria-hidden="true" className={`grid place-items-center bg-bg-3 font-bold text-fg-2 ${avatarClassName}`}>+{hidden}</span>
      )}
    </Tooltip>
  )
}
