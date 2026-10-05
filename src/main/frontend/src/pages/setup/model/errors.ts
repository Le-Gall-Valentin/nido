export class SetupCodeInvalidError extends Error {
  constructor() { super('Setup code invalid'); this.name = 'SetupCodeInvalidError' }
}

/** Someone finished the setup meanwhile — or it was never due. */
export class SetupAlreadyDoneError extends Error {
  constructor() { super('Setup already done'); this.name = 'SetupAlreadyDoneError' }
}

export class KeyNotSavedError extends Error {
  constructor() { super('Encryption key not saved'); this.name = 'KeyNotSavedError' }
}
