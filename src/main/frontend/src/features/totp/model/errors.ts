export class TotpCodeError extends Error {
  constructor() { super('Invalid TOTP code'); this.name = 'TotpCodeError' }
}

export class TotpChallengeExpiredError extends Error {
  constructor() { super('TOTP challenge expired'); this.name = 'TotpChallengeExpiredError' }
}

export class TotpAlreadyEnabledError extends Error {
  constructor() { super('TOTP is already enabled'); this.name = 'TotpAlreadyEnabledError' }
}

export class TotpMaxAttemptsError extends Error {
  constructor() { super('Too many incorrect TOTP attempts'); this.name = 'TotpMaxAttemptsError' }
}

export class TotpConfirmMaxAttemptsError extends Error {
  constructor() { super('Too many failed confirmation attempts'); this.name = 'TotpConfirmMaxAttemptsError' }
}

export class TotpDisableMaxAttemptsError extends Error {
  constructor() { super('Too many failed disable attempts'); this.name = 'TotpDisableMaxAttemptsError' }
}