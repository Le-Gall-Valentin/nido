import { createContext, useContext, type ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'

const FinanceApiContext = createContext<IFinanceApi | null>(null)

interface FinanceApiProviderProps {
  api: IFinanceApi
  children: ReactNode
}

/** Injects the IFinanceApi implementation consumed by the Finance page's hooks. */
export function FinanceApiProvider({ api, children }: FinanceApiProviderProps) {
  return <FinanceApiContext.Provider value={api}>{children}</FinanceApiContext.Provider>
}

export function useFinanceApi(): IFinanceApi {
  const api = useContext(FinanceApiContext)
  if (!api) {
    throw new Error('useFinanceApi must be used within a FinanceApiProvider')
  }
  return api
}
