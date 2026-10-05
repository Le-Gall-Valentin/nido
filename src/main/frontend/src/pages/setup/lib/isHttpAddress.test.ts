import { describe, expect, it } from 'vitest'
import { isHttpAddress } from './isHttpAddress'

describe('isHttpAddress', () => {
  it('accepts a full http or https address, whatever the host', () => {
    expect(isHttpAddress('https://nido.example.com')).toBe(true)
    expect(isHttpAddress(' http://192.168.1.10:8080/ ')).toBe(true)
  })

  it('refuses what the server would refuse', () => {
    for (const typed of ['', 'nido.example.com', 'ftp://nido.example.com', 'https://', 'mailto:jane@example.com',
      'https://example.com/nido', 'https://example.com/nido/', 'https://nido.example.com/?a=b', 'https://nido.example.com/#top',
      'https://jane:pw@nido.example.com']) {
      expect(isHttpAddress(typed)).toBe(false)
    }
  })
})
