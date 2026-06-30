import type { LucideIcon } from 'lucide-react'
import { Users } from 'lucide-react'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  to: string
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
  sectionKey: string
}

export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'shell:users', sectionKey: 'nav.section.admin', adminOnly: true, to: ROUTES.ADMIN_USERS, icon: Users, labelKey: 'nav.users' },
]