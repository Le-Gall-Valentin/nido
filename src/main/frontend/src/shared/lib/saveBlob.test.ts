import { afterEach, describe, expect, it, vi } from 'vitest'
import { saveBlob } from './saveBlob'

describe('saveBlob', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('saves under the name given, from a link in the page, and lets the file go only later', () => {
    vi.useFakeTimers()
    const blob = new Blob(['BEGIN:VCALENDAR'])
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:x')
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    let clicked: { download: string; href: string | null; inThePage: boolean } | null = null
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked = { download: this.download, href: this.getAttribute('href'), inThePage: document.body.contains(this) }
    })

    saveBlob(blob, 'nido-chez-nous-2026-10-13.ics')

    expect(create).toHaveBeenCalledWith(blob)
    expect(clicked).toEqual({ download: 'nido-chez-nous-2026-10-13.ics', href: 'blob:x', inThePage: true })
    expect(document.querySelector('a[download]')).toBeNull()
    expect(revoke).not.toHaveBeenCalled()
    vi.advanceTimersByTime(60_000)
    expect(revoke).toHaveBeenCalledWith('blob:x')
  })
})
