export { NetworkError, ServerError, RateLimitError } from '@/shared/lib'

export class CredentialsError extends Error {
  constructor() { super('Invalid credentials'); this.name = 'CredentialsError' }
}