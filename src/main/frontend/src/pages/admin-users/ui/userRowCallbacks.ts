import type { AdminUser } from '@/entities/user'

/**
 * Per-user action callbacks shared by the two list layouts (UsersTable rows and
 * UsersCardList cards). Adding a new per-user action is a single edit here plus
 * the action button in UserActions — the layouts only differ in chrome.
 */
export interface UserRowCallbacks {
  onToggleActive: (user: AdminUser) => void
  onEditRole: (user: AdminUser) => void
  onResetTotp: (user: AdminUser) => void
  onDelete: (user: AdminUser) => void
}
