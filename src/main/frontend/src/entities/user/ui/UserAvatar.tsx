import { avatarHue } from '../lib/avatarHue'
import { getInitials } from '../lib/getInitials'

/** The person's hue and a neighbour of it; the lightness and chroma come from the theme (index.css). */
function gradientFor(userId: string): string {
  const hue = avatarHue(userId)
  return `linear-gradient(135deg, oklch(var(--avatar-l-from) var(--avatar-c) ${hue}), oklch(var(--avatar-l-to) var(--avatar-c) ${hue + 25}))`
}

interface UserAvatarProps {
  /** Whose avatar it is: their colour comes from it, so two people with the same initials still look apart. */
  userId: string
  username: string
  /** Tailwind classes controlling size and shape (e.g. "size-6 rounded-md text-[10px]"). */
  className?: string
}

export function UserAvatar({ userId, username, className = 'size-10 rounded-full text-[13px]' }: UserAvatarProps) {
  return (
    <div
      className={`shrink-0 flex items-center justify-center font-semibold text-white ${className}`}
      style={{ background: gradientFor(userId) }}
      aria-hidden="true"
    >
      {getInitials(username)}
    </div>
  )
}
