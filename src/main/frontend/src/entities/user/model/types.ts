export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'USER'

export interface User {
  id: string
  username: string
  email: string
  role: UserRole
  createdAt: string
  totpEnabled: boolean
}

/** User as seen by admin endpoints — includes account state. */
export interface AdminUser extends User {
  isActive: boolean
}

export function isAdminRole(role?: UserRole): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}