import { useQuery } from '@tanstack/react-query'
import { capabilitiesApi } from '../api/capabilitiesApi'
import type { Capabilities, ICapabilitiesApi } from './ICapabilitiesApi'

export type Availability = 'loading' | 'available' | 'unavailable'
export type PasswordResetAvailability = Availability
export type MailAvailability = Availability

export const CAPABILITIES_KEY = ['auth', 'capabilities'] as const

/**
 * One answer of the server, asked again on each visit: mail can be switched on or off from the settings
 * page, without a redeploy. A failed request counts as unavailable, so a hiccup shows each page exactly as
 * it is without mail. Both hooks below read the same request.
 */
function useCapability(capability: keyof Capabilities, api: ICapabilitiesApi): Availability {
  const { data, isPending } = useQuery({
    queryKey: CAPABILITIES_KEY,
    queryFn: () => api.capabilities(),
    staleTime: 0,
    retry: false,
  })
  if (isPending) return 'loading'
  return data?.[capability] === true ? 'available' : 'unavailable'
}

/** Whether this installation can send a reset link — which is whether mail is configured on the server. */
export function usePasswordResetAvailability(api: ICapabilitiesApi = capabilitiesApi): PasswordResetAvailability {
  return useCapability('passwordReset', api)
}

/** Whether mail is configured — what the administration tells an administrator about who will be told. */
export function useMailAvailability(api: ICapabilitiesApi = capabilitiesApi): MailAvailability {
  return useCapability('mail', api)
}
