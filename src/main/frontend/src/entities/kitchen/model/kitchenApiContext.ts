import { createContext, useContext } from 'react'
import type { IKitchenApi } from './IKitchenApi'

export const KitchenApiContext = createContext<IKitchenApi | null>(null)

export function useKitchenApi(): IKitchenApi {
  const api = useContext(KitchenApiContext)
  if (!api) {
    throw new Error('useKitchenApi must be used within a KitchenApiProvider')
  }
  return api
}
