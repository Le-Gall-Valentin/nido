import { useQuery } from '@tanstack/react-query'
import { passwordResetApi } from '../api/passwordResetApi'
import type { IPasswordResetApi } from './IPasswordResetApi'
import { InvalidResetLinkError } from './errors'

export type ResetLinkState = 'checking' | 'valid' | 'invalid' | 'unavailable'

/**
 * Whether a reset link still works, asked once when the page opens — so nobody types a new password
 * twice to learn the link had expired. A link the server refuses is invalid; a server that could not
 * be asked leaves it unavailable, and {@link retry} asks again.
 *
 * Through the query cache rather than an effect of its own: a second mount (React looking for side
 * effects in development) joins the request in flight instead of sending another. Nothing is kept
 * once the page is left.
 */
export function useResetLinkCheck(
  token: string | null,
  api: IPasswordResetApi = passwordResetApi,
): { state: ResetLinkState; retry: () => void } {
  const { isPending, isError, error, refetch } = useQuery({
    queryKey: ['auth', 'reset-link', token],
    queryFn: async () => {
      await api.checkToken(token as string)
      return true
    },
    enabled: token !== null,
    retry: false,
    staleTime: Infinity,
    gcTime: 0,
  })
  const retry = () => void refetch()

  if (token === null) return { state: 'invalid', retry }
  if (isPending) return { state: 'checking', retry }
  if (isError) return { state: error instanceof InvalidResetLinkError ? 'invalid' : 'unavailable', retry }
  return { state: 'valid', retry }
}
