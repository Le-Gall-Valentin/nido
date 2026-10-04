import type { NotificationPreferences, PreferenceChange, PreferenceTarget, TypePreference } from './types'

/** The card after one switch: the named channel or kind takes the new state, everything else is kept as is. */
export function applyChange(preferences: NotificationPreferences, { target, enabled }: PreferenceChange): NotificationPreferences {
  if (target.kind === 'channel') {
    return {
      ...preferences,
      channels: preferences.channels.map((c) => (c.channel === target.code ? { ...c, enabled } : c)),
    }
  }
  return {
    ...preferences,
    types: preferences.types.map((t) => (t.type === target.code ? { ...t, enabled } : t)),
  }
}

/** One key per switch, to tell which ones are waiting for the server. */
export function targetKey(target: PreferenceTarget): string {
  return `${target.kind}:${target.code}`
}

/** `space.invitation` → `space`: kinds are grouped by the context they come from, in the server's order. */
export function groupTypes(types: TypePreference[]): { group: string; types: TypePreference[] }[] {
  const groups: { group: string; types: TypePreference[] }[] = []
  for (const preference of types) {
    const dot = preference.type.indexOf('.')
    const group = dot < 0 ? preference.type : preference.type.slice(0, dot)
    const last = groups[groups.length - 1]
    if (last?.group === group) last.types.push(preference)
    else groups.push({ group, types: [preference] })
  }
  return groups
}
