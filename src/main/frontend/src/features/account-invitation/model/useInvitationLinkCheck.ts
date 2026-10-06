import { useQuery } from '@tanstack/react-query'
import { InvalidLinkError, type LinkCheckState } from '@/shared/lib'
import { accountInvitationApi } from '../api/accountInvitationApi'
import type { IAccountInvitationApi } from './IAccountInvitationApi'

/**
 * Whether an invitation link still works, and whose it is — asked once when the page opens, like a reset
 * link: through the query cache, so a second mount joins the request in flight.
 */
export function useInvitationLinkCheck(
  token: string | null,
  api: IAccountInvitationApi = accountInvitationApi,
): { state: LinkCheckState; username: string | null; retry: () => void } {
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['auth', 'invitation-link', token],
    queryFn: () => api.checkInvitation(token as string),
    enabled: token !== null,
    retry: false,
    staleTime: Infinity,
    gcTime: 0,
  })
  const retry = () => void refetch()

  if (token === null) return { state: 'invalid', username: null, retry }
  if (isPending) return { state: 'checking', username: null, retry }
  if (isError) return { state: error instanceof InvalidLinkError ? 'invalid' : 'unavailable', username: null, retry }
  return { state: 'valid', username: data, retry }
}
