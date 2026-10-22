import { describe, expect, it, vi } from 'vitest'
import type { TFunction } from 'i18next'
import { saveBlob } from '@/shared/lib'
import { downloadKeyFile, keyFileText } from './keyFile'

vi.mock('@/shared/lib', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/shared/lib')>()),
  saveBlob: vi.fn(),
}))

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
  it('saves the text as a plain text file named for the key', async () => {
    downloadKeyFile('the key file')

    const [blob, filename] = vi.mocked(saveBlob).mock.calls[0] ?? []
    expect(filename).toBe('nido-encryption-key.txt')
    expect(blob?.type).toBe('text/plain;charset=utf-8')
    expect(await blob?.text()).toBe('the key file')
  })
})
