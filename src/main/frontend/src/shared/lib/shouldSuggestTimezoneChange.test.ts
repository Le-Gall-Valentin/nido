import { describe, expect, it } from 'vitest'
import { shouldSuggestTimezoneChange } from './shouldSuggestTimezoneChange'

describe('shouldSuggestTimezoneChange', () => {
  const personalInParis = { type: 'PERSONAL' as const, timezone: 'Europe/Paris' }

  it('offers to move a personal space when its owner is plainly elsewhere', () => {
    expect(shouldSuggestTimezoneChange({
      space: personalInParis, browser: 'America/Toronto', dismissedFor: null,
    })).toBe(true)
  })

  it('says nothing when the space already keeps the browser calendar', () => {
    expect(shouldSuggestTimezoneChange({
      space: personalInParis, browser: 'Europe/Paris', dismissedFor: null,
    })).toBe(false)
  })

  it('never offers to move a shared space', () => {
    // A shared calendar belongs to every member. One of them opening the app from a hotel must not
    // be invited to change the day the rent falls due for everybody else — that decision lives in
    // the space's settings, deliberately, where it reads as what it is.
    expect(shouldSuggestTimezoneChange({
      space: { type: 'SHARED', timezone: 'Europe/Paris' }, browser: 'America/Toronto', dismissedFor: null,
    })).toBe(false)
  })

  it('stays quiet about a zone that was already turned down', () => {
    // Two weeks abroad should ask once, not every morning.
    expect(shouldSuggestTimezoneChange({
      space: personalInParis, browser: 'America/Toronto', dismissedFor: 'America/Toronto',
    })).toBe(false)
  })

  it('asks again when the traveller has moved on somewhere new', () => {
    // Turning down Toronto says nothing about Tokyo, and the refusal must not become permanent
    // silence — somebody who actually moves country has to be asked.
    expect(shouldSuggestTimezoneChange({
      space: personalInParis, browser: 'Asia/Tokyo', dismissedFor: 'America/Toronto',
    })).toBe(true)
  })

  it('says nothing while the space is still loading', () => {
    expect(shouldSuggestTimezoneChange({
      space: undefined, browser: 'America/Toronto', dismissedFor: null,
    })).toBe(false)
  })
})
