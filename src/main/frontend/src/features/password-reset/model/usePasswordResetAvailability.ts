import { useQuery } from '@tanstack/react-query'
import { passwordResetApi } from '../api/passwordResetApi'
import type { IPasswordResetApi } from './IPasswordResetApi'

export type PasswordResetAvailability = 'loading' | 'available' | 'unavailable'

export const PASSWORD_RESET_CAPABILITY_KEY = ['auth', 'capabilities'] as const

/**
 * Whether this installation can send a reset link — which is whether mail is configured on the server.
 * Asked again on each visit: mail can be switched on or off from the settings page, without a redeploy. A failed request counts as
 * unavailable, so a hiccup shows the login page exactly as it is without mail.
 */
export function usePasswordResetAvailability(api: IPasswordResetApi = passwordResetApi): PasswordResetAvailability {
  const { data, isPending } = useQuery({
    queryKey: PASSWORD_RESET_CAPABILITY_KEY,
    queryFn: () => api.capabilities(),
    staleTime: 0,
    retry: false,
  })
  if (isPending) return 'loading'
  return data?.passwordReset === true ? 'available' : 'unavailable'
}
