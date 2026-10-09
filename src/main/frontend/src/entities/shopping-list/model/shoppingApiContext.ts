import { createContext, useContext } from 'react'
import type { IShoppingApi } from './IShoppingApi'

export const ShoppingApiContext = createContext<IShoppingApi | null>(null)

export function useShoppingApi(): IShoppingApi {
  const api = useContext(ShoppingApiContext)
  if (!api) {
    throw new Error('useShoppingApi must be used within a ShoppingApiProvider')
  }
  return api
}
