import type { ReactNode } from 'react'
import type { IKitchenApi } from './IKitchenApi'
import { KitchenApiContext } from './kitchenApiContext'

interface KitchenApiProviderProps {
  api: IKitchenApi
  children: ReactNode
}

/** Injects the IKitchenApi implementation consumed by this page's hooks. */
export function KitchenApiProvider({ api, children }: KitchenApiProviderProps) {
  return <KitchenApiContext.Provider value={api}>{children}</KitchenApiContext.Provider>
}
