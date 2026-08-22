// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import {
  spacesPageApi,
  SpaceNotAccessibleError,
  InsufficientRoleError,
  SelfManagementError,
  OwnerProtectedError,
  SpaceRoleAlreadyAssignedError,
  LastOwnerError,
  MemberNotFoundError,
  AlreadyMemberError,
  InvitationAlreadyPendingError,
  InvitationNotFoundError,
  InvitationNotPendingError,
  InvitationExpiredError,
  PersonalSpaceImmutableError,
  NoAccountForEmailError,
} from './spacesPageApi'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

const mock = client as unknown as Record<string, ReturnType<typeof vi.fn>>

function axiosErr(status: number, title?: string): AxiosError {
  return new axios.AxiosError('err', undefined, undefined, undefined, {
    status, data: title ? { title } : {}, headers: {}, config: {} as never, statusText: String(status),
  })
}

beforeEach(() => { Object.values(mock).forEach(m => m.mockReset()) })

const SPACE_DETAIL = {
  id: 's-1', type: 'SHARED', name: 'Chez nous', description: null,
  accent: '#c17a5c', glyph: '🏡', myRole: 'OWNER', memberCount: 1,
}

describe('getSpaceDetail', () => {
  it('GET /spaces/{id}', async () => {
    mock.get.mockResolvedValue({ data: SPACE_DETAIL })
    const result = await spacesPageApi.getSpaceDetail('s-1')
    expect(mock.get).toHaveBeenCalledWith('/spaces/s-1')
    expect(result).toEqual(SPACE_DETAIL)
  })

  it('throws SpaceNotAccessibleError on 404', async () => {
    mock.get.mockRejectedValue(axiosErr(404, 'SpaceNotFound'))
    await expect(spacesPageApi.getSpaceDetail('s-1')).rejects.toBeInstanceOf(SpaceNotAccessibleError)
  })

  it('throws NetworkError on no response', async () => {
    mock.get.mockRejectedValue(new Error('network'))
    await expect(spacesPageApi.getSpaceDetail('s-1')).rejects.toBeInstanceOf(NetworkError)
  })

  it('throws SpaceNotAccessibleError on a plain 404 (SpaceNotFound / NotAMember, indistinguishable)', async () => {
    mock.get.mockRejectedValue(axiosErr(404))
    await expect(spacesPageApi.getSpaceDetail('s-1')).rejects.toBeInstanceOf(SpaceNotAccessibleError)
  })
})

describe('listMembers', () => {
  it('GET /spaces/{id}/members', async () => {
    mock.get.mockResolvedValue({ data: [] })
    await spacesPageApi.listMembers('s-1')
    expect(mock.get).toHaveBeenCalledWith('/spaces/s-1/members')
  })

  it('throws InsufficientRoleError on 403', async () => {
    mock.get.mockRejectedValue(axiosErr(403, 'InsufficientRole'))
    await expect(spacesPageApi.listMembers('s-1')).rejects.toBeInstanceOf(InsufficientRoleError)
  })
})

describe('listInvitations', () => {
  it('GET /spaces/{id}/invitations', async () => {
    mock.get.mockResolvedValue({ data: [] })
    await spacesPageApi.listInvitations('s-1')
    expect(mock.get).toHaveBeenCalledWith('/spaces/s-1/invitations')
  })

  it('throws RateLimitError on 429', async () => {
    mock.get.mockRejectedValue(axiosErr(429))
    await expect(spacesPageApi.listInvitations('s-1')).rejects.toBeInstanceOf(RateLimitError)
  })
})

describe('listReceivedInvitations', () => {
  it('GET /me/invitations', async () => {
    mock.get.mockResolvedValue({ data: [] })
    await spacesPageApi.listReceivedInvitations()
    expect(mock.get).toHaveBeenCalledWith('/me/invitations')
  })

  it('throws ServerError on 500', async () => {
    mock.get.mockRejectedValue(axiosErr(500))
    await expect(spacesPageApi.listReceivedInvitations()).rejects.toBeInstanceOf(ServerError)
  })
})

describe('createSpace', () => {
  it('POST /spaces with body', async () => {
    mock.post.mockResolvedValue({ data: SPACE_DETAIL })
    await spacesPageApi.createSpace({ name: 'Chez nous', description: 'x', accent: '#c17a5c', glyph: '🏡' })
    expect(mock.post).toHaveBeenCalledWith('/spaces', { name: 'Chez nous', description: 'x', accent: '#c17a5c', glyph: '🏡' })
  })
})

describe('updateSpace', () => {
  it('PATCH /spaces/{id} sends only provided fields', async () => {
    mock.patch.mockResolvedValue({ status: 204 })
    await spacesPageApi.updateSpace('s-1', { name: 'New name' })
    expect(mock.patch).toHaveBeenCalledWith('/spaces/s-1', { name: 'New name' })
  })

  it('omits absent fields entirely, including when clearing description with an empty string', async () => {
    mock.patch.mockResolvedValue({ status: 204 })
    await spacesPageApi.updateSpace('s-1', { name: 'New name', description: '' })
    // accent and glyph are absent from the input and must not appear in the body at all.
    expect(mock.patch).toHaveBeenCalledWith('/spaces/s-1', { name: 'New name', description: '' })
  })

  it('throws PersonalSpaceImmutableError on 422', async () => {
    mock.patch.mockRejectedValue(axiosErr(422, 'PersonalSpaceImmutable'))
    await expect(spacesPageApi.updateSpace('s-1', { name: 'x' })).rejects.toBeInstanceOf(PersonalSpaceImmutableError)
  })
})

describe('deleteSpace', () => {
  it('DELETE /spaces/{id}', async () => {
    mock.delete.mockResolvedValue({ status: 204 })
    await spacesPageApi.deleteSpace('s-1')
    expect(mock.delete).toHaveBeenCalledWith('/spaces/s-1')
  })
})

describe('changeMemberRole', () => {
  it('PATCH /spaces/{id}/members/{userId} with role', async () => {
    mock.patch.mockResolvedValue({ status: 204 })
    await spacesPageApi.changeMemberRole('s-1', 'u-1', 'ADMIN')
    expect(mock.patch).toHaveBeenCalledWith('/spaces/s-1/members/u-1', { role: 'ADMIN' })
  })

  it('throws SpaceRoleAlreadyAssignedError on 409 RoleAlreadyAssigned', async () => {
    mock.patch.mockRejectedValue(axiosErr(409, 'RoleAlreadyAssigned'))
    await expect(spacesPageApi.changeMemberRole('s-1', 'u-1', 'ADMIN')).rejects.toBeInstanceOf(SpaceRoleAlreadyAssignedError)
  })

  it('throws SelfManagementError on 409 SelfManagementForbidden', async () => {
    mock.patch.mockRejectedValue(axiosErr(409, 'SelfManagementForbidden'))
    await expect(spacesPageApi.changeMemberRole('s-1', 'u-1', 'ADMIN')).rejects.toBeInstanceOf(SelfManagementError)
  })

  it('throws OwnerProtectedError on 409 OwnerMembershipProtected', async () => {
    mock.patch.mockRejectedValue(axiosErr(409, 'OwnerMembershipProtected'))
    await expect(spacesPageApi.changeMemberRole('s-1', 'u-1', 'ADMIN')).rejects.toBeInstanceOf(OwnerProtectedError)
  })

  it('throws ServerError on an unrecognized 409 title', async () => {
    mock.patch.mockRejectedValue(axiosErr(409, 'SomethingElse'))
    await expect(spacesPageApi.changeMemberRole('s-1', 'u-1', 'ADMIN')).rejects.toBeInstanceOf(ServerError)
  })
})

describe('removeMember', () => {
  it('DELETE /spaces/{id}/members/{userId}', async () => {
    mock.delete.mockResolvedValue({ status: 204 })
    await spacesPageApi.removeMember('s-1', 'u-1')
    expect(mock.delete).toHaveBeenCalledWith('/spaces/s-1/members/u-1')
  })

  it('throws MemberNotFoundError on 404 MemberNotFound, distinct from a vanished space', async () => {
    mock.delete.mockRejectedValue(axiosErr(404, 'MemberNotFound'))
    await expect(spacesPageApi.removeMember('s-1', 'u-1')).rejects.toBeInstanceOf(MemberNotFoundError)
  })
})

describe('transferOwnership', () => {
  it('POST /spaces/{id}/members/{userId}/ownership', async () => {
    mock.post.mockResolvedValue({ status: 204 })
    await spacesPageApi.transferOwnership('s-1', 'u-1')
    expect(mock.post).toHaveBeenCalledWith('/spaces/s-1/members/u-1/ownership')
  })
})

describe('leaveSpace', () => {
  it('DELETE /spaces/{id}/membership', async () => {
    mock.delete.mockResolvedValue({ status: 204 })
    await spacesPageApi.leaveSpace('s-1')
    expect(mock.delete).toHaveBeenCalledWith('/spaces/s-1/membership')
  })

  it('throws LastOwnerError on 409 LastOwnerCannotLeave', async () => {
    mock.delete.mockRejectedValue(axiosErr(409, 'LastOwnerCannotLeave'))
    await expect(spacesPageApi.leaveSpace('s-1')).rejects.toBeInstanceOf(LastOwnerError)
  })
})

describe('inviteMember', () => {
  it('POST /spaces/{id}/invitations with body', async () => {
    mock.post.mockResolvedValue({ data: {} })
    await spacesPageApi.inviteMember('s-1', 'carol@example.fr', 'MEMBER')
    expect(mock.post).toHaveBeenCalledWith('/spaces/s-1/invitations', { email: 'carol@example.fr', role: 'MEMBER' })
  })

  it('throws AlreadyMemberError on 409 AlreadyMember', async () => {
    mock.post.mockRejectedValue(axiosErr(409, 'AlreadyMember'))
    await expect(spacesPageApi.inviteMember('s-1', 'carol@example.fr', 'MEMBER')).rejects.toBeInstanceOf(AlreadyMemberError)
  })

  it('throws InvitationAlreadyPendingError on 409 InvitationAlreadyPending', async () => {
    mock.post.mockRejectedValue(axiosErr(409, 'InvitationAlreadyPending'))
    await expect(spacesPageApi.inviteMember('s-1', 'carol@example.fr', 'MEMBER')).rejects.toBeInstanceOf(InvitationAlreadyPendingError)
  })

  it('throws NoAccountForEmailError on 422 NoAccountForEmail', async () => {
    mock.post.mockRejectedValue(axiosErr(422, 'NoAccountForEmail'))
    await expect(spacesPageApi.inviteMember('s-1', 'carol@example.fr', 'MEMBER')).rejects.toBeInstanceOf(NoAccountForEmailError)
  })
})

describe('revokeInvitation', () => {
  it('DELETE /spaces/{id}/invitations/{invitationId}', async () => {
    mock.delete.mockResolvedValue({ status: 204 })
    await spacesPageApi.revokeInvitation('s-1', 'i-1')
    expect(mock.delete).toHaveBeenCalledWith('/spaces/s-1/invitations/i-1')
  })

  it('throws InvitationNotFoundError on 404 InvitationNotFound', async () => {
    mock.delete.mockRejectedValue(axiosErr(404, 'InvitationNotFound'))
    await expect(spacesPageApi.revokeInvitation('s-1', 'i-1')).rejects.toBeInstanceOf(InvitationNotFoundError)
  })

  it('throws InvitationNotPendingError on 409 InvitationNotPending', async () => {
    mock.delete.mockRejectedValue(axiosErr(409, 'InvitationNotPending'))
    await expect(spacesPageApi.revokeInvitation('s-1', 'i-1')).rejects.toBeInstanceOf(InvitationNotPendingError)
  })
})

describe('acceptInvitation', () => {
  it('POST /invitations/{id}/accept and returns the joined spaceId', async () => {
    mock.post.mockResolvedValue({ data: { spaceId: 's-2' } })
    const result = await spacesPageApi.acceptInvitation('i-1')
    expect(mock.post).toHaveBeenCalledWith('/invitations/i-1/accept')
    expect(result).toEqual({ spaceId: 's-2' })
  })

  it('throws InvitationExpiredError on 422 InvitationExpired', async () => {
    mock.post.mockRejectedValue(axiosErr(422, 'InvitationExpired'))
    await expect(spacesPageApi.acceptInvitation('i-1')).rejects.toBeInstanceOf(InvitationExpiredError)
  })

  it('throws InvitationNotFoundError on 404 InvitationNotFound', async () => {
    mock.post.mockRejectedValue(axiosErr(404, 'InvitationNotFound'))
    await expect(spacesPageApi.acceptInvitation('i-1')).rejects.toBeInstanceOf(InvitationNotFoundError)
  })
})
