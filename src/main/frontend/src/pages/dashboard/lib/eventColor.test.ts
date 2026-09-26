import { describe, it, expect } from 'vitest'
import { eventBarClass, eventTintClass } from './eventColor'

describe('eventColor', () => {
  it('paints an event in its own colour', () => {
    expect(eventBarClass('event-indigo')).toBe('bg-event-indigo')
    expect(eventTintClass('event-cyan')).toBe('bg-event-cyan-dim text-event-cyan')
  })

  it('falls back to violet for no colour or a colour it does not know', () => {
    expect(eventBarClass(null)).toBe('bg-event-violet')
    expect(eventBarClass('#ff0000')).toBe('bg-event-violet')
    expect(eventTintClass('status-red')).toBe('bg-event-violet-dim text-event-violet')
  })
})
