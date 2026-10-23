import type { TFunction } from 'i18next'
import { saveBlob } from '@/shared/lib'

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

/** Saves the key file. */
export function downloadKeyFile(text: string): void {
  saveBlob(new Blob([text], { type: 'text/plain;charset=utf-8' }), 'nido-encryption-key.txt')
}
