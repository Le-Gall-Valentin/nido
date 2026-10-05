import { useQuery } from '@tanstack/react-query'
import { setupApi } from '../api/setupApi'
import type { ISetupApi } from './ISetupApi'
import type { SetupStatus } from './types'

export const SETUP_STATUS_KEY = ['setup', 'status'] as const

export type SetupStatusState =
  | { state: 'loading' }
  | { state: 'required'; status: SetupStatus }
  | { state: 'done' }

/**
 * Asked once per load, before anything else. A server that cannot answer counts as set up: the
 * application then shows its own errors, rather than a setup screen nobody can use.
 */
export function useSetupStatus(api: ISetupApi = setupApi): SetupStatusState {
  const { data, isPending, isError } = useQuery({
    queryKey: SETUP_STATUS_KEY,
    queryFn: () => api.status(),
    staleTime: Infinity,
    retry: 1,
  })
  if (isPending) return { state: 'loading' }
  if (isError || !data?.required) return { state: 'done' }
  return { state: 'required', status: data }
}
