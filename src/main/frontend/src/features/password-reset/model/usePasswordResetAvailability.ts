import { useQuery } from '@tanstack/react-query'
import { passwordResetApi } from '../api/passwordResetApi'
import type { IPasswordResetApi, PasswordResetCapabilities } from './IPasswordResetApi'

export type PasswordResetAvailability = 'loading' | 'available' | 'unavailable'
export type MailAvailability = PasswordResetAvailability

export const PASSWORD_RESET_CAPABILITY_KEY = ['auth', 'capabilities'] as const

/**
 * One answer of the server, asked again on each visit: mail can be switched on or off from the settings
 * page, without a redeploy. A failed request counts as unavailable, so a hiccup shows each page exactly as
 * it is without mail. Both hooks below read the same request.
 */
function useCapability(capability: keyof PasswordResetCapabilities, api: IPasswordResetApi): PasswordResetAvailability {
  const { data, isPending } = useQuery({
    queryKey: PASSWORD_RESET_CAPABILITY_KEY,
    queryFn: () => api.capabilities(),
    staleTime: 0,
    retry: false,
  })
  if (isPending) return 'loading'
  return data?.[capability] === true ? 'available' : 'unavailable'
}

/** Whether this installation can send a reset link — which is whether mail is configured on the server. */
export function usePasswordResetAvailability(api: IPasswordResetApi = passwordResetApi): PasswordResetAvailability {
  return useCapability('passwordReset', api)
}

/** Whether mail is configured — what the administration tells an administrator about who will be told. */
export function useMailAvailability(api: IPasswordResetApi = passwordResetApi): MailAvailability {
  return useCapability('mail', api)
}
