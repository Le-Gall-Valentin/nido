import { useState } from 'react'
import { NetworkError, RateLimitError } from '@/shared/lib'
import { notificationPreferencesApi } from '../api/notificationPreferencesApi'
import type { INotificationPreferencesApi } from './INotificationPreferencesApi'
import { targetKey } from './preferenceChange'
import type { PreferenceTarget } from './types'
import { useToggleNotificationPreference } from './useToggleNotificationPreference'

export type SwitchFailure = 'rate_limit' | 'network' | 'server'

function failureOf(error: unknown): SwitchFailure {
  if (error instanceof RateLimitError) return 'rate_limit'
  if (error instanceof NetworkError) return 'network'
  return 'server'
}

/**
 * The card's switches in motion: which ones wait for the server, and why the last change failed. A switch
 * is held only while its own request runs; a new change clears the previous failure.
 */
export function usePreferenceSwitches(api: INotificationPreferencesApi = notificationPreferencesApi) {
  const toggle = useToggleNotificationPreference(api)
  const [pending, setPending] = useState<ReadonlySet<string>>(new Set())
  const [failure, setFailure] = useState<SwitchFailure | null>(null)

  async function change(target: PreferenceTarget, enabled: boolean) {
    const key = targetKey(target)
    setFailure(null)
    setPending((current) => new Set(current).add(key))
    try {
      await toggle.mutateAsync({ target, enabled })
    } catch (error) {
      setFailure(failureOf(error))
    } finally {
      setPending((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }

  return { isPending: (target: PreferenceTarget) => pending.has(targetKey(target)), failure, change }
}
