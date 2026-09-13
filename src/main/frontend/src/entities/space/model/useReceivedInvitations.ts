import { useQuery } from '@tanstack/react-query'
import { useSpaceApi } from './spaceApiContext'

export const RECEIVED_INVITATIONS_QUERY_KEY = ['invitations', 'received'] as const

export function useReceivedInvitations() {
  const api = useSpaceApi()
  return useQuery({
    queryKey: RECEIVED_INVITATIONS_QUERY_KEY,
    queryFn: () => api.listReceivedInvitations(),
  })
}
