import type { Category } from '@/entities/finance'
import { resolveCategoryIcon } from '../lib/resolveCategoryIcon'

/** A colored, rounded-square category badge — the icon rendered in white over the category's own color, matching how a member avatar is a colored circle with initials. */
export function CategoryIconBadge({ category, size = 38 }: { category?: Category; size?: number }) {
  const Icon = resolveCategoryIcon(category?.icon ?? 'Circle')
  return (
    <div
      className="grid shrink-0 place-items-center rounded-[10px] text-white"
      style={{ width: size, height: size, backgroundColor: category?.color ?? 'var(--color-fg-3)' }}
    >
      <Icon size={Math.round(size * 0.5)} />
    </div>
  )
}
