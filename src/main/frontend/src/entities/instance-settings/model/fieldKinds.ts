export type FieldKind =
  | { kind: 'text' }
  | { kind: 'number' }
  | { kind: 'password' }
  | { kind: 'switch' }
  | { kind: 'select'; options: readonly string[] }

/** How each setting is typed in — the server's catalogue decides which settings exist. */
export const FIELD_KINDS: Record<string, FieldKind> = {
  'mail.host': { kind: 'text' },
  'mail.port': { kind: 'number' },
  'mail.security': { kind: 'select', options: ['starttls', 'tls', 'none'] },
  'mail.username': { kind: 'text' },
  'mail.password': { kind: 'password' },
  'mail.from': { kind: 'text' },
  'public-url': { kind: 'text' },
  'sessions.access-token-minutes': { kind: 'number' },
  'sessions.refresh-token-days': { kind: 'number' },
  'api.swagger': { kind: 'switch' },
}
