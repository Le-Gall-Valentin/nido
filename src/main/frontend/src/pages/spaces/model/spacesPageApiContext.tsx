import { createContext, useContext, type ReactNode } from 'react'
import type { ISpacesPageApi } from './ISpacesPageApi'

const SpacesPageApiContext = createContext<ISpacesPageApi | null>(null)

interface SpacesPageApiProviderProps {
  api: ISpacesPageApi
  children: ReactNode
}

/** Injects the ISpacesPageApi implementation consumed by this page's hooks. */
export function SpacesPageApiProvider({ api, children }: SpacesPageApiProviderProps) {
  return <SpacesPageApiContext.Provider value={api}>{children}</SpacesPageApiContext.Provider>
}

export function useSpacesPageApi(): ISpacesPageApi {
  const api = useContext(SpacesPageApiContext)
  if (!api) {
    throw new Error('useSpacesPageApi must be used within a SpacesPageApiProvider')
  }
  return api
}
