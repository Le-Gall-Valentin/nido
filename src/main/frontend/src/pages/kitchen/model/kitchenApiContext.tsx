import { createContext, useContext, type ReactNode } from 'react'
import type { IKitchenApi } from './IKitchenApi'

const KitchenApiContext = createContext<IKitchenApi | null>(null)

interface KitchenApiProviderProps {
  api: IKitchenApi
  children: ReactNode
}

/** Injects the IKitchenApi implementation consumed by this page's hooks. */
export function KitchenApiProvider({ api, children }: KitchenApiProviderProps) {
  return <KitchenApiContext.Provider value={api}>{children}</KitchenApiContext.Provider>
}

export function useKitchenApi(): IKitchenApi {
  const api = useContext(KitchenApiContext)
  if (!api) {
    throw new Error('useKitchenApi must be used within a KitchenApiProvider')
  }
  return api
}
