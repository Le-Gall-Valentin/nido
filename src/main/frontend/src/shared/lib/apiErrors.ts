export class NetworkError extends Error {
  constructor() { super('Network error'); this.name = 'NetworkError' }
}

export class ServerError extends Error {
  constructor() { super('Server error'); this.name = 'ServerError' }
}

export class ForbiddenError extends Error {
  constructor() { super('Forbidden'); this.name = 'ForbiddenError' }
}

export class NotFoundError extends Error {
  constructor() { super('Not found'); this.name = 'NotFoundError' }
}

export class RateLimitError extends Error {
  readonly retryAfterSeconds: number | null
  constructor(retryAfterSeconds: number | null = null) {
    super('Too many requests')
    this.name = 'RateLimitError'
    this.retryAfterSeconds = retryAfterSeconds
  }
}
/** A one-time link — reset or invitation — that expired, was used, was replaced or never existed: 410. */
export class InvalidLinkError extends Error {
  constructor() { super('This link is no longer valid'); this.name = 'InvalidLinkError' }
}

/** A password the server's rules refuse. */
export class WeakPasswordError extends Error {
  constructor() { super('Password does not follow the rules'); this.name = 'WeakPasswordError' }
}
