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

export function downloadKeyFile(text: string): void {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = 'nido-encryption-key.txt'
  link.click()
  URL.revokeObjectURL(url)
}
