import { afterEach, describe, expect, it, vi } from 'vitest'
import { saveBlob } from './saveBlob'

describe('saveBlob', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('saves under the name given, from a link in the page, and lets the file go only later', () => {
    vi.useFakeTimers()
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:x')
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    let download = ''
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      download = this.download
    })

    saveBlob(new Blob(['BEGIN:VCALENDAR']), 'nido-chez-nous-2026-10-13.ics')

    expect(download).toBe('nido-chez-nous-2026-10-13.ics')
    expect(revoke).not.toHaveBeenCalled()
    vi.advanceTimersByTime(60_000)
    expect(revoke).toHaveBeenCalledWith('blob:x')
  })
})
