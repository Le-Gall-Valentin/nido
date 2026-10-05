import { afterEach, describe, expect, it, vi } from 'vitest'
import type { TFunction } from 'i18next'
import { downloadKeyFile, keyFileText } from './keyFile'

const t = ((key: string, options?: Record<string, string>) => (options ? `${key} ${Object.values(options).join(' ')}` : key)) as unknown as TFunction

describe('keyFileText', () => {
  it('puts the key alone on its line, with the address and what the key protects', () => {
    const lines = keyFileText('the-key', 'https://nido.example.com', new Date(2026, 9, 4), t).split('\n')

    expect(lines[2]).toBe('the-key')
    expect(lines).toContain('key.file.url https://nido.example.com')
    expect(lines).toContain('key.file.protects')
  })
})

describe('downloadKeyFile', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('keeps the file alive until the browser has fetched it, from a link in the page', () => {
    vi.useFakeTimers()
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:the-key')
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    let attached = false
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      attached = document.body.contains(this)
    })

    downloadKeyFile('the key file')

    expect(create).toHaveBeenCalled()
    expect(attached).toBe(true)
    expect(revoke).not.toHaveBeenCalled()
    vi.runAllTimers()
    expect(revoke).toHaveBeenCalledWith('blob:the-key')
    expect(document.querySelector('a[download]')).toBeNull()
  })
})
