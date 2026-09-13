import * as LucideIcons from 'lucide-react'

const EXCLUDED_EXPORTS = new Set(['createLucideIcon', 'icons', 'default'])

/**
 * Every distinct lucide-react icon component name, deduplicated and sorted
 * alphabetically — the source list for the icon picker grid.
 *
 * lucide-react exports each icon under three aliases (e.g. "LucideWallet",
 * "Wallet", "WalletIcon"), all pointing to the same component. Grouping by
 * component reference and preferring the bare canonical name (neither the
 * "Lucide"-prefixed nor the "Icon"-suffixed alias) keeps exactly one,
 * human-recognizable entry per icon.
 */
export const ALL_ICON_NAMES: string[] = (() => {
  // A non-empty tuple, because that is what a group is: it is created with one alias and only ever
  // grows. Stated in the type, the first element needs no check at the point of use.
  const groups = new Map<unknown, [string, ...string[]]>()
  for (const [name, value] of Object.entries(LucideIcons)) {
    if (!/^[A-Z]/.test(name) || EXCLUDED_EXPORTS.has(name)) continue
    if (typeof value !== 'object' && typeof value !== 'function') continue
    const aliases = groups.get(value)
    if (aliases) aliases.push(name)
    else groups.set(value, [name])
  }
  const names = Array.from(groups.values(), (aliases) => {
    const canonical = aliases.find((n) => !n.startsWith('Lucide') && !n.endsWith('Icon'))
    if (canonical) return canonical
    // Reduce rather than sort-and-take-first: it starts from an element that exists, so the result
    // is a string without the compiler having to trust an index.
    return aliases.reduce((shortest, n) => (n.length < shortest.length ? n : shortest))
  })
  return names.sort()
})()
