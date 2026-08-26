import type { SpaceSummary } from '../model/types'
import { safeAccent, safeGlyph } from '../lib/spaceAppearance'

export type SpaceAvatarSize = 'sm' | 'md' | 'lg'

const SIZE_CLASS: Record<SpaceAvatarSize, string> = {
  sm: 'size-6 rounded-md text-[13px]',
  md: 'size-9 rounded-lg text-base',
  lg: 'size-14 rounded-xl text-2xl',
}

interface SpaceAvatarProps {
  space: Pick<SpaceSummary, 'accent' | 'glyph'>
  /** `sm` for the topbar, `md` for a list, `lg` for a page header. */
  size?: SpaceAvatarSize
}

export function SpaceAvatar({ space, size = 'md' }: SpaceAvatarProps) {
  return (
    <div
      className={`shrink-0 flex items-center justify-center ${SIZE_CLASS[size]}`}
      style={{ background: safeAccent(space.accent) }}
      aria-hidden="true"
    >
      {safeGlyph(space.glyph)}
    </div>
  )
}
