import { useReceivedInvitations } from './useReceivedInvitations'

export function useHasPendingInvitations(): boolean {
  const { data } = useReceivedInvitations()
  return (data?.length ?? 0) > 0
}
