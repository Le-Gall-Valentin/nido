import { describe, it, expect } from 'vitest'
import { openingMinutes } from './openingTime'

const h = (hhmm: string) => { const [a, b] = hhmm.split(':').map(Number); return a * 60 + b }
const NINE_HOURS = 9 * 60

describe('openingMinutes — where the week or day grid opens', () => {
  it('opens at 08:00 when nothing has a time', () => {
    expect(openingMinutes([], NINE_HOURS)).toBe(h('08:00'))
  })

  it('opens half an hour before a lone event', () => {
    expect(openingMinutes([h('14:00')], NINE_HOURS)).toBe(h('13:30'))
  })

  it("opens on the stretch of the screen's height that holds the most events", () => {
    // 09:00, 10:00 and 11:00 fit in one screenful; 19:00 is on its own.
    expect(openingMinutes([h('19:00'), h('09:00'), h('10:00'), h('11:00')], NINE_HOURS)).toBe(h('08:30'))
    // Here the evening wins: three events against one.
    expect(openingMinutes([h('08:00'), h('18:00'), h('19:00'), h('20:00')], NINE_HOURS)).toBe(h('17:30'))
  })

  it('takes the earliest stretch when two hold as many', () => {
    expect(openingMinutes([h('20:00'), h('09:00')], NINE_HOURS)).toBe(h('08:30'))
  })

  it('never opens before midnight', () => {
    expect(openingMinutes([h('00:15')], NINE_HOURS)).toBe(0)
  })
})
