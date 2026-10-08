import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { AdminUser, InvitationDelivery } from '../model/types'
import type { IAdminUsersApi, UsersPage } from '../model/IAdminUsersApi'

export type { AdminUser }
export type { UsersPage }

export class ConflictError extends Error {
  constructor() { super('Username or email already taken'); this.name = 'ConflictError' }
}

export class AlreadyJoinedError extends Error {
  constructor() { super('This account already chose its password'); this.name = 'AlreadyJoinedError' }
}

export class AlreadyInactiveError extends Error {
  constructor() { super('This account is already deactivated'); this.name = 'AlreadyInactiveError' }
}

export class RoleAlreadyAssignedError extends Error {
  constructor() { super('User already has this role'); this.name = 'RoleAlreadyAssignedError' }
}

function handleError(error: unknown, onConflict?: () => never): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 409 && onConflict) onConflict()
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const adminUsersApi: IAdminUsersApi = {
  async listUsers(page: number, size = 20, search?: string): Promise<UsersPage> {
    try {
      const params: Record<string, string | number> = { page, size }
      if (search) params.search = search
      const res = await client.get<UsersPage>('/users', { params })
      return res.data
    } catch (error) {
      handleError(error)
    }
  },

  async createUser(username: string, email: string, role: 'USER' | 'ADMIN'): Promise<InvitationDelivery> {
    try {
      const res = await client.post<{ invitation: InvitationDelivery }>('/users', { username, email, role })
      return res.data.invitation
    } catch (error) {
      handleError(error, () => { throw new ConflictError() })
    }
  },

  async resendInvitation(id: string): Promise<InvitationDelivery> {
    try {
      const res = await client.post<InvitationDelivery>(`/users/${id}/invitation`)
      return res.data
    } catch (error) {
      handleError(error, () => { throw new AlreadyJoinedError() })
    }
  },

  async updateUserRole(id: string, role: 'USER' | 'ADMIN'): Promise<void> {
    try {
      await client.patch(`/users/${id}`, { role })
    } catch (error) {
      handleError(error, () => { throw new RoleAlreadyAssignedError() })
    }
  },

  async activateUser(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/activate`)
    } catch (error) {
      handleError(error)
    }
  },

  async deactivateUser(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/deactivate`)
    } catch (error) {
      handleError(error, () => { throw new AlreadyInactiveError() })
    }
  },

  async resetTotp(id: string): Promise<void> {
    try {
      await client.post(`/users/${id}/2fa/reset`, { methods: ['APP', 'MAIL'] })
    } catch (error) {
      handleError(error)
    }
  },

  async deleteUser(id: string): Promise<void> {
    try {
      await client.delete(`/users/${id}`)
    } catch (error) {
      handleError(error)
    }
  },
}
