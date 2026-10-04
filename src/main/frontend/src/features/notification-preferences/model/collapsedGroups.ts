const KEY = 'nido.notification-groups.collapsed'

/**
 * The categories of the notifications card folded on this device. localStorage throws outright in a
 * private window and holds anything after a manual edit: every access is guarded, and anything unexpected
 * reads as "nothing folded" — a preference about layout must never break the card.
 */
export function readCollapsedGroups(): Set<string> {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return new Set()
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return new Set()
    return new Set(parsed.filter((group): group is string => typeof group === 'string'))
  } catch {
    return new Set()
  }
}

export function writeCollapsedGroups(groups: ReadonlySet<string>): void {
  try {
    localStorage.setItem(KEY, JSON.stringify([...groups]))
  } catch {
    // A fold we cannot persist still holds for this visit.
  }
}
