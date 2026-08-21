import type { LucideIcon } from 'lucide-react'
import { Settings, Shield } from 'lucide-react'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  to: string
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
}

export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'shell:account', to: ROUTES.ACCOUNT, icon: Settings, labelKey: 'nav.settings' },
  { id: 'shell:users', adminOnly: true, to: ROUTES.ADMIN_USERS, icon: Shield, labelKey: 'nav.administration' },
]
