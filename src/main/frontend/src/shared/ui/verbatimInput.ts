/**
 * For a field whose value must reach the server exactly as typed — a username, an address, a password:
 * no capital letter added by a phone keyboard, no autocorrection, no spelling marks. Without it, Chrome
 * and Safari capitalise the first letter on virtual keyboards, so "jane" arrives as "Jane" — a username
 * nobody has. Fields of type email, url or password are never capitalised, whatever the attribute.
 */
export const VERBATIM_INPUT_PROPS = {
  autoCapitalize: 'off',
  autoCorrect: 'off',
  spellCheck: false,
} as const
