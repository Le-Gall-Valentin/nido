/** The code is wrong — or, the app's, expired or already used. */
export class CodeError extends Error {
  constructor() { super('Invalid code'); this.name = 'CodeError' }
}

/** The sign-in waited too long between the password and the code: start again. */
export class ChallengeExpiredError extends Error {
  constructor() { super('Two-factor challenge expired'); this.name = 'ChallengeExpiredError' }
}

/** Too many wrong codes at sign-in: the account waits a while. */
export class MaxAttemptsError extends Error {
  constructor() { super('Too many incorrect codes'); this.name = 'MaxAttemptsError' }
}

/**
 * Too many wrong codes: a code sent is gone with them — ask for a new one —, the app's are refused for a quarter
 * of an hour.
 */
export class CodeSpentError extends Error {
  constructor() { super('Too many wrong codes'); this.name = 'CodeSpentError' }
}

/** No code sent is waiting any more — never asked for, or past its ten minutes: ask for a new one. */
export class CodeExpiredError extends Error {
  constructor() { super('No code is waiting any more'); this.name = 'CodeExpiredError' }
}

/** Too many wrong first codes: the enrolment is cancelled and starts again. */
export class ConfirmMaxAttemptsError extends Error {
  constructor() { super('Too many failed confirmation attempts'); this.name = 'ConfirmMaxAttemptsError' }
}

/** Nothing to confirm: the enrolment never started, or expired. */
export class EnrolmentExpiredError extends Error {
  constructor() { super('No enrolment under way'); this.name = 'EnrolmentExpiredError' }
}

/** The method cannot be used now — the mail while mail is off. */
export class MethodUnavailableError extends Error {
  constructor() { super('Method unavailable'); this.name = 'MethodUnavailableError' }
}

/** The method is not on — removed by an administrator meanwhile, say. */
export class MethodNotEnabledError extends Error {
  constructor() { super('Method not enabled'); this.name = 'MethodNotEnabledError' }
}

export class MethodAlreadyEnabledError extends Error {
  constructor() { super('Method already enabled'); this.name = 'MethodAlreadyEnabledError' }
}

/** A code was sent moments ago — it still works; another can be asked for after `seconds`. */
export class ResendTooSoonError extends Error {
  readonly seconds: number
  constructor(seconds: number) { super('A code was sent moments ago'); this.name = 'ResendTooSoonError'; this.seconds = seconds }
}

/** The account was sent all the codes its window allows; the next can leave after `seconds`. */
export class SendLimitError extends Error {
  readonly seconds: number
  constructor(seconds: number) { super('Too many codes sent by mail'); this.name = 'SendLimitError'; this.seconds = seconds }
}
