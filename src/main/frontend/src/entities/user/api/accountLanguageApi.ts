import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, ServerError, type Language } from '@/shared/lib'
import type { IAccountLanguageApi } from '../model/IAccountLanguageApi'

export const accountLanguageApi: IAccountLanguageApi = {
  async saveLanguage(language: Language): Promise<void> {
    try {
      await client.put('/users/me/language', { language })
    } catch (error) {
      if (isAxiosError(error) && error.response?.status !== undefined) throw new ServerError()
      throw new NetworkError()
    }
  },
}
