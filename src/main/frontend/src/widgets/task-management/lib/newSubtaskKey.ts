let lastKey = 0

/**
 * A row key for a subtask that has no id yet. A counter rather than crypto.randomUUID, which a
 * browser only offers in a secure context — not to the app opened over plain http on a LAN address.
 */
export function newSubtaskKey(): string {
  lastKey += 1
  return `new-${lastKey}`
}
