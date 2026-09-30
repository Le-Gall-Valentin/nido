import type { Language } from '@/shared/lib'

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  createdAt: string
  totpEnabled: boolean
  /** The language recorded on the account; null until a signed-in session records one. */
  language?: Language | null
}

/** User as seen by admin endpoints — includes account state. */
export interface AdminUser extends User {
  isActive: boolean
}

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}