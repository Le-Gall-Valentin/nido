import { createContext, useContext, type ReactNode } from 'react'
import type { IShoppingApi } from './IShoppingApi'

const ShoppingApiContext = createContext<IShoppingApi | null>(null)

interface ShoppingApiProviderProps {
  api: IShoppingApi
  children: ReactNode
}

/** Injects the IShoppingApi implementation consumed by the shopping-list page and the menu-export feature's hooks. */
export function ShoppingApiProvider({ api, children }: ShoppingApiProviderProps) {
  return <ShoppingApiContext.Provider value={api}>{children}</ShoppingApiContext.Provider>
}

export function useShoppingApi(): IShoppingApi {
  const api = useContext(ShoppingApiContext)
  if (!api) {
    throw new Error('useShoppingApi must be used within a ShoppingApiProvider')
  }
  return api
}
