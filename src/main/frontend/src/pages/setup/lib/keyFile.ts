import type { TFunction } from 'i18next'

/** The text of the key file: the key on its own line, so that it is copied whole, then what it is for. */
export function keyFileText(key: string, publicUrl: string, createdAt: Date, t: TFunction): string {
  return [
    t('key.file.title'),
    '',
    key,
    '',
    t('key.file.date', { date: createdAt.toLocaleString() }),
    t('key.file.url', { url: publicUrl }),
    t('key.file.protects'),
    t('key.file.where'),
    '',
  ].join('\n')
}

/** How long the file stays fetchable after the click: Firefox and Safari read it after click() returns. */
const KEEP_FILE_MS = 60_000

/**
 * Saves the key file. The link is put in the page for the click — some browsers ignore a click on a
 * detached link — and the file is released only later: released at once, the download can fail.
 */
export function downloadKeyFile(text: string): void {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'nido-encryption-key.txt'
  link.hidden = true
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), KEEP_FILE_MS)
}
