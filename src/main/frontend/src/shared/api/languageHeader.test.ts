// @vitest-environment node
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { attachLanguageHeader } from './languageHeader'

async function headerSentWith(language: string | undefined): Promise<unknown> {
  let seen: InternalAxiosRequestConfig | undefined
  const instance = axios.create({
    adapter: async (config) => {
      seen = config
      return { data: null, status: 204, statusText: 'No Content', headers: {}, config }
    },
  })
  attachLanguageHeader(instance, () => language)
  await instance.get('/anything')
  return seen?.headers.get('Accept-Language')
}

describe('attachLanguageHeader', () => {
  it('sends the language on screen, without its region', async () => {
    expect(await headerSentWith('fr-FR')).toBe('fr')
    expect(await headerSentWith('en-GB')).toBe('en')
  })

  it('sends English for any language the app does not speak, or none', async () => {
    expect(await headerSentWith('de')).toBe('en')
    expect(await headerSentWith(undefined)).toBe('en')
  })
})
