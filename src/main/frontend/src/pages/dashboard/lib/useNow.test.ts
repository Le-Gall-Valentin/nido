import { describe, it, expect } from 'vitest'
import { nowInZone } from './useNow'

describe('nowInZone', () => {
  it('reads date and time on the space calendar, not the machine one', () => {
    const instant = new Date('2026-09-26T22:30:00Z')
    expect(nowInZone(instant, 'Europe/Paris')).toEqual({ date: '2026-09-27', time: '00:30' })
    expect(nowInZone(instant, 'America/Toronto')).toEqual({ date: '2026-09-26', time: '18:30' })
  })
})
