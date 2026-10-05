/** The server refused some settings; `errors` maps a setting's code to a problem code (see common:setting_problem). */
export class SettingsInvalidError extends Error {
  readonly errors: Record<string, string>
  constructor(errors: Record<string, string>) {
    super('Some settings are not valid')
    this.name = 'SettingsInvalidError'
    this.errors = errors
  }
}

/** The setting is set by a variable of the environment and cannot be changed from a page. */
export class SettingLockedError extends Error {
  readonly setting: string | null
  constructor(setting: string | null) {
    super('Setting locked by the environment')
    this.name = 'SettingLockedError'
    this.setting = setting
  }
}

/**
 * The test mail did not leave. `reason` says why (see common:mail_failure); `serverReply` is what the
 * mail server answered, when it answered as a mail server does — null otherwise, never another
 * service's words.
 */
export class MailTestFailedError extends Error {
  readonly reason: string
  readonly serverReply: string | null
  constructor(reason: string, serverReply: string | null) {
    super('Test mail failed')
    this.name = 'MailTestFailedError'
    this.reason = reason
    this.serverReply = serverReply
  }
}
