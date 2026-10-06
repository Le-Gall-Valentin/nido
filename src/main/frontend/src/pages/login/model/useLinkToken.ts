import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { tokenInHash } from './linkToken'

/**
 * The token of a one-time link — reset or invitation — read from the part after '#', which the browser never
 * sends to a server, then taken out of the address bar: it stays in no history entry and on no screen shared
 * later. Kept once the bar is clean; but a newer link opened in the same tab arrives without a reload, and it
 * is that link the person means now.
 *
 * The link can also stop working while the password is typed — the save is refused with 410: {@link refuseOnSave}
 * marks this token as refused.
 */
export function useLinkToken(): { token: string | null; refusedOnSave: boolean; refuseOnSave: () => void } {
  const location = useLocation()
  const navigate = useNavigate()
  const tokenInAddress = tokenInHash(location.hash)
  const [token, setToken] = useState(tokenInAddress)
  if (tokenInAddress !== null && tokenInAddress !== token) setToken(tokenInAddress)
  const [refusedOnSaveFor, setRefusedOnSaveFor] = useState<string | null>(null)

  useEffect(() => {
    if (location.hash) void navigate({ pathname: location.pathname, search: location.search }, { replace: true })
  }, [location.hash, location.pathname, location.search, navigate])

  return {
    token,
    refusedOnSave: token !== null && refusedOnSaveFor === token,
    refuseOnSave: () => setRefusedOnSaveFor(token),
  }
}
