import type { LucideIcon } from 'lucide-react'
import { Lock, Settings, Shield, SlidersHorizontal, User, Users } from 'lucide-react'
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
//
// The mockup has exactly one "Membres & groupes" entry, no separate
// per-space "Membres" item: /spaces already lets you drill into a group to
// reach its members page (SpaceListSection → SpaceMembersPage), which is
// the same "click a group card to open its detail" flow the mockup uses.
export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'nav:spaces', to: () => ROUTES.SPACES, icon: Users, labelKey: 'nav.groups' },
  {
    // The mockup's fourth sub-category, Notifications, has no backing feature
    // (no notification system exists yet) — only the three that map to real
    // account content are listed here.
    id: 'nav:account', to: () => ROUTES.ACCOUNT_PROFILE, icon: Settings, labelKey: 'nav.settings',
    children: [
      { id: 'nav:account:profile', to: () => ROUTES.ACCOUNT_PROFILE, icon: User, labelKey: 'nav.settings_profile' },
      { id: 'nav:account:security', to: () => ROUTES.ACCOUNT_SECURITY, icon: Lock, labelKey: 'nav.settings_security' },
      { id: 'nav:account:preferences', to: () => ROUTES.ACCOUNT_PREFERENCES, icon: SlidersHorizontal, labelKey: 'nav.settings_preferences' },
    ],
  },
  { id: 'nav:users', adminOnly: true, to: () => ROUTES.ADMIN_USERS, icon: Shield, labelKey: 'nav.administration' },
]
