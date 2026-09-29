export class InvalidResetLinkError extends Error {
  constructor() { super('Password reset link is no longer valid'); this.name = 'InvalidResetLinkError' }
}

export class WeakPasswordError extends Error {
  constructor() { super('Password does not follow the rules'); this.name = 'WeakPasswordError' }
}
