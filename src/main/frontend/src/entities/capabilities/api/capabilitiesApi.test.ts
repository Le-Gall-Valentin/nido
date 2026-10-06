// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { client } from '@/shared/api'
import { ServerError } from '@/shared/lib'
import { capabilitiesApi } from './capabilitiesApi'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn() } }))

const mocked = client as unknown as { get: ReturnType<typeof vi.fn> }

describe('capabilitiesApi', () => {
  beforeEach(() => { mocked.get.mockReset() })

  it('asks what this installation can do', async () => {
    mocked.get.mockResolvedValue({ data: { passwordReset: true, mail: true } })

    await expect(capabilitiesApi.capabilities()).resolves.toEqual({ passwordReset: true, mail: true })
    expect(mocked.get).toHaveBeenCalledWith('/auth/capabilities')
  })

  it('reads a failed answer as an error of its kind', async () => {
    mocked.get.mockRejectedValueOnce(new axios.AxiosError('error', undefined, undefined, undefined, {
      status: 500, data: {}, headers: {}, config: {} as never, statusText: '500',
    }))

    await expect(capabilitiesApi.capabilities()).rejects.toBeInstanceOf(ServerError)
  })
})
