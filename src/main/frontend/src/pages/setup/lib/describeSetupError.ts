import { NetworkError, RateLimitError } from '@/shared/lib'
import { KeyNotSavedError, SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'

/** The translation key (namespace `setup`) that words an error of the setup routes. */
export function describeSetupError(error: unknown): string {
  if (error instanceof SetupCodeInvalidError) return 'errors.code_invalid'
  if (error instanceof SetupAlreadyDoneError) return 'errors.already_done'
  if (error instanceof KeyNotSavedError) return 'errors.key_not_saved'
  if (error instanceof RateLimitError) return 'errors.too_many'
  if (error instanceof NetworkError) return 'errors.network'
  return 'errors.server'
}
