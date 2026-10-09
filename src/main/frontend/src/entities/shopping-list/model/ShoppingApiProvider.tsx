import type { ReactNode } from 'react'
import type { IShoppingApi } from './IShoppingApi'
import { ShoppingApiContext } from './shoppingApiContext'

interface ShoppingApiProviderProps {
  api: IShoppingApi
  children: ReactNode
}

/** Injects the IShoppingApi implementation consumed by the shopping-list page and the menu-export feature's hooks. */
export function ShoppingApiProvider({ api, children }: ShoppingApiProviderProps) {
  return <ShoppingApiContext.Provider value={api}>{children}</ShoppingApiContext.Provider>
}
