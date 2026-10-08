import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { TwoFactorMethod, User } from '@/entities/user'
import {
  MethodAlreadyEnabledError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, commonErrorMessage,
  type ITwoFactorMethodsApi, type MailSetupData, type MethodState,
} from '@/features/two-factor'
import { useFlash } from './useFlash'

const METHODS_KEY = ['two-factor', 'methods'] as const
const ORDER: TwoFactorMethod[] = ['APP', 'MAIL']

export type OpenDialog =
  | { dialog: 'enable_app' }
  | { dialog: 'enable_mail'; setup: MailSetupData }
  | { dialog: 'disable'; method: TwoFactorMethod; paused: boolean; resendAfterSeconds: number }

interface UseTwoFactorMethodsOptions {
  user: User
  onPatch: (partial: Partial<User>) => void
  api: ITwoFactorMethodsApi
}

/**
 * The account's methods and what turning one on or off sets in motion — which code leaves, which dialog opens,
 * what is said after. The section only shows it.
 */
export function useTwoFactorMethods({ user, onPatch, api }: UseTwoFactorMethodsOptions) {
  const queryClient = useQueryClient()
  // Read again each time the section shows: an administrator, another tab or an address saved while mail was off
  // may have changed them since.
  const { data: methods, isError } = useQuery({ queryKey: METHODS_KEY, queryFn: () => api.list(), staleTime: 0 })
  const [open, setOpen] = useState<OpenDialog | null>(null)
  const [busy, setBusy] = useState(false)
  const { flash, showFlash } = useFlash(4000)

  const reload = () => { void queryClient.invalidateQueries({ queryKey: METHODS_KEY }) }

  function settle(method: TwoFactorMethod, enabled: boolean) {
    const on = new Set(user.twoFactorMethods)
    if (enabled) on.add(method)
    else on.delete(method)
    onPatch({ twoFactorMethods: ORDER.filter(m => on.has(m)) })
    reload()
  }

  function showError(error: unknown) {
    if (error instanceof MethodUnavailableError) {
      showFlash('error', 'twofa.error.mail_unavailable')
      reload()
    } else if (error instanceof MethodAlreadyEnabledError || error instanceof MethodNotEnabledError) {
      // Turned on or off meanwhile — in another tab, or by an administrator: the list shows where things stand.
      showFlash('error', 'twofa.error.changed')
      reload()
    } else {
      const message = commonErrorMessage(error)
      showFlash('error', message.key, message.values)
    }
  }

  // The mail's code leaves on the click, never from an effect: StrictMode would send it twice.
  async function enable(method: TwoFactorMethod) {
    if (method === 'APP') { setOpen({ dialog: 'enable_app' }); return }
    setBusy(true)
    try {
      setOpen({ dialog: 'enable_mail', setup: await api.setupMail() })
    } catch (error) {
      if (error instanceof ResendTooSoonError) setOpen({ dialog: 'enable_mail', setup: { sentTo: user.email, resendAfterSeconds: error.seconds } })
      else showError(error)
    } finally {
      setBusy(false)
    }
  }

  async function disable(state: MethodState) {
    const paused = state.enabled && !state.usable
    if (state.method === 'APP' || paused) {
      setOpen({ dialog: 'disable', method: state.method, paused, resendAfterSeconds: 0 })
      return
    }
    setBusy(true)
    try {
      const { resendAfterSeconds } = await api.sendDisableCode()
      setOpen({ dialog: 'disable', method: 'MAIL', paused: false, resendAfterSeconds })
    } catch (error) {
      if (error instanceof ResendTooSoonError) setOpen({ dialog: 'disable', method: 'MAIL', paused: false, resendAfterSeconds: error.seconds })
      else showError(error)
    } finally {
      setBusy(false)
    }
  }

  function enabled(method: TwoFactorMethod) {
    settle(method, true)
    setOpen(null)
    showFlash('success', method === 'APP' ? 'twofa.success_enabled_app' : 'twofa.success_enabled_mail')
  }

  function disabled(method: TwoFactorMethod) {
    settle(method, false)
    setOpen(null)
    showFlash('success', 'twofa.success_disabled')
  }

  /** A paused method's dialog met a method that came back meanwhile: it wants a code the dialog has no field for. */
  function stale() {
    setOpen(null)
    showFlash('error', 'twofa.error.changed')
    reload()
  }

  return {
    methods, isError, busy, open, flash,
    close: () => setOpen(null), enable, disable, enabled, disabled, stale,
  }
}
