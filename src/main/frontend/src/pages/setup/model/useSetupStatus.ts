import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { setupApi } from '../api/setupApi'
import { knownSetUp, rememberSetUp } from '../lib/installationHint'
import type { ISetupApi } from './ISetupApi'
import type { SetupStatus } from './types'

export const SETUP_STATUS_KEY = ['setup', 'status'] as const

export type SetupStatusState =
  | { state: 'loading' }
  | { state: 'required'; status: SetupStatus }
  | { state: 'done' }

const SET_UP: SetupStatus = { required: false, lockedPublicUrl: null, mailLocked: false }

/**
 * Asked once per load, before anything else. A server that cannot answer counts as set up: the
 * application then shows its own errors, rather than a setup screen nobody can use. Where this browser
 * already saw the installation set up, the application opens at once and the answer checks it behind.
 */
export function useSetupStatus(api: ISetupApi = setupApi): SetupStatusState {
  const { data, isPending, isError, isPlaceholderData } = useQuery({
    queryKey: SETUP_STATUS_KEY,
    queryFn: () => api.status(),
    staleTime: Infinity,
    retry: 1,
    placeholderData: knownSetUp() ? SET_UP : undefined,
  })
  useEffect(() => {
    if (data && !isPlaceholderData) rememberSetUp(!data.required)
  }, [data, isPlaceholderData])
  if (isPending) return { state: 'loading' }
  if (isError || !data?.required) return { state: 'done' }
  return { state: 'required', status: data }
}
