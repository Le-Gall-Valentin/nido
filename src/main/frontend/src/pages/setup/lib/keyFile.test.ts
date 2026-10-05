import { describe, expect, it } from 'vitest'
import type { TFunction } from 'i18next'
import { keyFileText } from './keyFile'

const t = ((key: string, options?: Record<string, string>) => (options ? `${key} ${Object.values(options).join(' ')}` : key)) as unknown as TFunction

describe('keyFileText', () => {
  it('puts the key alone on its line, with the address and what the key protects', () => {
    const lines = keyFileText('the-key', 'https://nido.example.com', new Date(2026, 9, 4), t).split('\n')

    expect(lines[2]).toBe('the-key')
    expect(lines).toContain('key.file.url https://nido.example.com')
    expect(lines).toContain('key.file.protects')
  })
})
