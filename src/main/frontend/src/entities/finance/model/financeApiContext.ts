import { createContext, useContext } from 'react'
import type { IFinanceApi } from './IFinanceApi'

export const FinanceApiContext = createContext<IFinanceApi | null>(null)

export function useFinanceApi(): IFinanceApi {
  const api = useContext(FinanceApiContext)
  if (!api) {
    throw new Error('useFinanceApi must be used within a FinanceApiProvider')
  }
  return api
}
