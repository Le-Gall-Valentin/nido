import type { Language } from '@/shared/lib'

export interface SetupStatus {
  required: boolean
  /** Set by NIDO_APP_URL: shown, not asked. */
  lockedPublicUrl: string | null
  /** Mail set by the environment: the wizard does not ask for it. */
  mailLocked: boolean
}

/** Settings by their code: `mail.host`, `mail.port`, … */
export type SettingValues = Record<string, string>

export interface SetupAdmin {
  username: string
  email: string
  password: string
  language: Language
}

export interface CompleteSetupRequest {
  code: string
  admin: SetupAdmin
  publicUrl: string
  /** null: mail left for later. */
  mail: SettingValues | null
  encryptionKeySaved: boolean
}

export type SetupKey = { source: 'GENERATED'; key: string } | { source: 'PROVIDED'; key: null }

export interface SetupMailTest {
  code: string
  mail: SettingValues
  publicUrl: string
  recipient: string
  language: Language
}
