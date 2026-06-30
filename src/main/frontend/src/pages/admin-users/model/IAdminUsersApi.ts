import type { AdminUser } from '@/entities/user'

/** Paginated users payload returned by the backend (mirrors `PageResponse`). */
export interface UsersPage {
  content: AdminUser[]
  totalElements: number
  /**
   * Zero-based page index echoed by the backend. The UI drives pagination from
   * its own `page` state, so this field is part of the wire contract rather
   * than a value the page reads back.
   */
  page: number
  size: number
}

/**
 * Port for admin user management. Consumers (hooks) depend on this contract,
 * never on the concrete axios-backed implementation, which is injected through
 * AdminUsersApiProvider.
 */
export interface IAdminUsersApi {
  listUsers(page: number, size?: number, search?: string): Promise<UsersPage>
  createUser(username: string, email: string, password: string, role: 'USER' | 'ADMIN'): Promise<void>
  updateUserRole(id: string, role: 'USER' | 'ADMIN'): Promise<void>
  activateUser(id: string): Promise<void>
  deactivateUser(id: string): Promise<void>
  resetTotp(id: string): Promise<void>
  deleteUser(id: string): Promise<void>
}
