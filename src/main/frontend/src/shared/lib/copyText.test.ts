import { afterEach, describe, expect, it, vi } from 'vitest'
import { copyText } from './copyText'

function clipboard(writeText: ((text: string) => Promise<void>) | undefined) {
  Object.defineProperty(navigator, 'clipboard', { value: writeText ? { writeText } : undefined, configurable: true })
}

function execCommand(result: boolean | 'throws') {
  const command = vi.fn(() => {
    if (result === 'throws') throw new Error('not supported')
    return result
  })
  Object.defineProperty(document, 'execCommand', { value: command, configurable: true })
  return command
}

describe('copyText', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    clipboard(undefined)
  })

  it('copies through the clipboard API where the page may use it', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    clipboard(writeText)

    expect(await copyText('the key')).toBe(true)
    expect(writeText).toHaveBeenCalledWith('the key')
  })

  it('falls back on a copy command over plain http, where the browser exposes no clipboard API', async () => {
    clipboard(undefined)
    let copied = ''
    const command = execCommand(true)
    command.mockImplementation(() => {
      copied = (document.activeElement as HTMLTextAreaElement).value.slice(
        (document.activeElement as HTMLTextAreaElement).selectionStart,
        (document.activeElement as HTMLTextAreaElement).selectionEnd,
      )
      return true
    })

    expect(await copyText('the key')).toBe(true)
    expect(command).toHaveBeenCalledWith('copy')
    expect(copied).toBe('the key')
    expect(document.querySelector('textarea')).toBeNull()
  })

  it('says so when nothing could copy', async () => {
    clipboard(vi.fn().mockRejectedValue(new Error('denied')))
    execCommand('throws')

    expect(await copyText('the key')).toBe(false)
    expect(document.querySelector('textarea')).toBeNull()
  })
})
