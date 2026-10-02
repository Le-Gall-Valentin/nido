import { describe, expect, it } from 'vitest'
import { applyChange, groupTypes, targetKey } from './preferenceChange'
import type { NotificationPreferences } from './types'

const PREFERENCES: NotificationPreferences = {
  channels: [{ channel: 'email', enabled: true }],
  types: [
    { type: 'agenda.reminder', enabled: true },
    { type: 'space.invitation', enabled: true },
    { type: 'space.removal', enabled: false },
  ],
}

describe('applyChange', () => {
  it('switches the one channel named, and nothing else', () => {
    const changed = applyChange(PREFERENCES, { target: { kind: 'channel', code: 'email' }, enabled: false })

    expect(changed.channels).toEqual([{ channel: 'email', enabled: false }])
    expect(changed.types).toBe(PREFERENCES.types)
  })

  it('switches the one kind named, and nothing else', () => {
    const changed = applyChange(PREFERENCES, { target: { kind: 'type', code: 'space.invitation' }, enabled: false })

    expect(changed.types.map((t) => t.enabled)).toEqual([true, false, false])
    expect(changed.channels).toBe(PREFERENCES.channels)
  })
})

describe('targetKey', () => {
  it('tells a channel and a kind of the same code apart', () => {
    expect(targetKey({ kind: 'channel', code: 'x.y' })).not.toBe(targetKey({ kind: 'type', code: 'x.y' }))
  })
})

describe('groupTypes', () => {
  it('groups the kinds by context, in the order the server gave', () => {
    expect(groupTypes(PREFERENCES.types)).toEqual([
      { group: 'agenda', types: [PREFERENCES.types[0]] },
      { group: 'space', types: [PREFERENCES.types[1], PREFERENCES.types[2]] },
    ])
  })
})
