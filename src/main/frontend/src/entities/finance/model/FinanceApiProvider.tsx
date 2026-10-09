import type { ReactNode } from 'react'
import type { IFinanceApi } from './IFinanceApi'
import { FinanceApiContext } from './financeApiContext'

interface FinanceApiProviderProps {
  api: IFinanceApi
  children: ReactNode
}

/** Injects the IFinanceApi implementation consumed by the Finance page's hooks. */
export function FinanceApiProvider({ api, children }: FinanceApiProviderProps) {
  return <FinanceApiContext.Provider value={api}>{children}</FinanceApiContext.Provider>
}
