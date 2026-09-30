// @vitest-environment node
import axios, { type InternalAxiosRequestConfig } from 'axios'
import i18n from 'i18next'
import { describe, expect, it } from 'vitest'
import { attachLanguageHeader } from './languageHeader'

function recordingInstance() {
  const sent: InternalAxiosRequestConfig[] = []
  const instance = axios.create({
    adapter: async (config) => {
      sent.push(config)
      return { data: null, status: 204, statusText: 'No Content', headers: {}, config }
    },
  })
  const lastHeader = () => sent.at(-1)?.headers.get('Accept-Language')
  return { instance, lastHeader }
}

async function headerSentWith(language: string | undefined): Promise<unknown> {
  const { instance, lastHeader } = recordingInstance()
  attachLanguageHeader(instance, () => language)
  await instance.get('/anything')
  return lastHeader()
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

  it('reads the language i18next shows, at each request, when told nothing else', async () => {
    // test-setup pins the default instance's language; setting it by hand stands in for a switch in
    // Preferences, which must reach the next request and not only the next page load.
    const shown = i18n as unknown as { language: string }
    const pinned = shown.language
    try {
      const { instance, lastHeader } = recordingInstance()
      attachLanguageHeader(instance)

      shown.language = 'fr'
      await instance.get('/anything')
      expect(lastHeader()).toBe('fr')

      shown.language = 'en-GB'
      await instance.get('/anything')
      expect(lastHeader()).toBe('en')
    } finally {
      shown.language = pinned
    }
  })
})
