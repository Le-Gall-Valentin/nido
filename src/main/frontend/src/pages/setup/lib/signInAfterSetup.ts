import { authApi } from '@/features/auth'
import { setSessionHint } from '@/shared/lib'

/** Signs the new administrator in; false when it cannot (the login page then takes over). */
export async function signInAfterSetup(identifier: string, password: string): Promise<boolean> {
  try {
    const result = await authApi.login({ identifier, password })
    if (result.type !== 'success') return false
    setSessionHint()
    return true
  } catch {
    return false
  }
}
