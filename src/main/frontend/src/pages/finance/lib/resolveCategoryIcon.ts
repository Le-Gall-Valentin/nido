import * as LucideIcons from 'lucide-react'
import { Circle, type LucideIcon } from 'lucide-react'

/** Categories store their icon as a lucide-react export name (e.g. "Home"); this resolves it at render time, falling back to a plain circle for a name that no longer matches an export. */
export function resolveCategoryIcon(iconName: string): LucideIcon {
  const icons = LucideIcons as unknown as Record<string, LucideIcon>
  return icons[iconName] ?? Circle
}
