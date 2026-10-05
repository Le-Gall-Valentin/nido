import type { AdminUser, InvitationDelivery } from './types'

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
  /** Creates the account and invites it: it chooses its own password with the link. */
  createUser(username: string, email: string, role: 'USER' | 'ADMIN'): Promise<InvitationDelivery>
  /** A new link for an account that has not chosen its password yet; the previous one stops working. */
  resendInvitation(id: string): Promise<InvitationDelivery>
  updateUserRole(id: string, role: 'USER' | 'ADMIN'): Promise<void>
  activateUser(id: string): Promise<void>
  deactivateUser(id: string): Promise<void>
  resetTotp(id: string): Promise<void>
  deleteUser(id: string): Promise<void>
}
