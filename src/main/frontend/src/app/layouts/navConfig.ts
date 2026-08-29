import type { LucideIcon } from 'lucide-react'
import { Settings, Shield, Users } from 'lucide-react'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  to: string
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
}

export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'shell:spaces', to: ROUTES.SPACES, icon: Users, labelKey: 'nav.groups' },
  { id: 'shell:account', to: ROUTES.ACCOUNT, icon: Settings, labelKey: 'nav.settings' },
  { id: 'shell:users', adminOnly: true, to: ROUTES.ADMIN_USERS, icon: Shield, labelKey: 'nav.administration' },
]

export interface SpaceNavItemConfig {
  id: string
  to: (spaceId: string) => string
  icon: LucideIcon
  labelKey: string
  children?: SpaceNavItemConfig[]
}

// Only modules that actually have a route/page belong here — no
// placeholder entries for future modules (Cuisine, Organisation, ...).
export const SPACE_NAV_CONFIG: SpaceNavItemConfig[] = [
  { id: 'space:members', to: ROUTES.spaceMembers, icon: Users, labelKey: 'nav.members' },
]
