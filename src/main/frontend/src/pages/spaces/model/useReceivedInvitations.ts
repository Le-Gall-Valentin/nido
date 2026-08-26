import { useQuery } from '@tanstack/react-query'
import { useSpacesPageApi } from './spacesPageApiContext'

export const RECEIVED_INVITATIONS_QUERY_KEY = ['invitations', 'received'] as const

export function useReceivedInvitations() {
  const api = useSpacesPageApi()
  return useQuery({
    queryKey: RECEIVED_INVITATIONS_QUERY_KEY,
    queryFn: () => api.listReceivedInvitations(),
  })
}
