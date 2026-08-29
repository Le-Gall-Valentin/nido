import type { LucideIcon } from 'lucide-react'
import { Settings, Shield, Users } from 'lucide-react'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  /**
   * Resolves the item's destination given the caller's current space
   * (see useCurrentSpaceId). Items that don't need a space (Groupes,
   * Compte, Administration) ignore the argument; items that do (Membres,
   * and future space modules) return undefined while it isn't resolved
   * yet, so the item is skipped rather than linking nowhere.
   */
  to: (spaceId: string | undefined) => string | undefined
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
  children?: NavItemConfig[]
}

// The sidebar shows this exact list at all times, for every authenticated
// route — it never depends on whether the current URL happens to carry a
// spaceId. Only modules that actually have a route/page belong here — no
// placeholder entries for future modules (Cuisine, Organisation, ...).
export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'nav:spaces', to: () => ROUTES.SPACES, icon: Users, labelKey: 'nav.groups' },
  {
    id: 'nav:members',
    to: (spaceId) => (spaceId ? ROUTES.spaceMembers(spaceId) : undefined),
    icon: Users,
    labelKey: 'nav.members',
  },
  { id: 'nav:account', to: () => ROUTES.ACCOUNT, icon: Settings, labelKey: 'nav.settings' },
  { id: 'nav:users', adminOnly: true, to: () => ROUTES.ADMIN_USERS, icon: Shield, labelKey: 'nav.administration' },
]
