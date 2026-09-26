import { useEffect, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'

/** The query parameter a link sets to ask a page to open its creation form on arrival. */
export const CREATE_INTENT_PARAM = 'create'

/** The creation forms a page can be asked to open: finance, shopping list, calendar. */
export type CreateIntent = 'transaction' | 'item' | 'event'

/** `path` with the parameter that asks its page to open the creation form for `intent`. */
export function withCreateIntent(path: string, intent: CreateIntent): string {
  return `${path}?${CREATE_INTENT_PARAM}=${intent}`
}

/**
 * Opens a page's creation form when the URL asks for it — once — then takes the request out of the URL.
 *
 * The dashboard's "Ajouter" menu cannot mount forms that live inside other pages (a page never imports
 * another page), so it links to the page with `?create=…` and the page opens its own form. `enabled`
 * holds the request until the caller knows it may write: the role arrives with the spaces list, after
 * the first render. The parameter leaves with `replace`, so neither Back nor a reload reopens the form.
 */
export function useCreateIntent(intent: CreateIntent, enabled: boolean, open: () => void): void {
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get(CREATE_INTENT_PARAM) === intent
  const openRef = useRef(open)

  useEffect(() => {
    openRef.current = open
  })

  useEffect(() => {
    if (!requested || !enabled) return
    openRef.current()
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.delete(CREATE_INTENT_PARAM)
      return next
    }, { replace: true })
  }, [requested, enabled, setSearchParams])
}
