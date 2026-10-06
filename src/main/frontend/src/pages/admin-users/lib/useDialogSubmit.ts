import { useRef, useState } from 'react'
import { mapApiErrorToKey } from './mapApiErrorToKey'

/**
 * What every administration dialog does when confirmed: run its action once — a double click sends nothing
 * twice — show it is busy, and say what went wrong in the dialog's own words (`<errorPrefix>.error.*`).
 */
export function useDialogSubmit(errorPrefix: string) {
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  async function submit(action: () => Promise<void>) {
    if (pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await action()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, errorPrefix))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  return { submit, isLoading, errorKey, clearError: () => setErrorKey(null) }
}
