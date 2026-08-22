import { createContext, useContext, type ReactNode } from 'react'
import type { ISpacesApi } from './ISpacesApi'

const SpacesApiContext = createContext<ISpacesApi | null>(null)

interface SpacesApiProviderProps {
  api: ISpacesApi
  children: ReactNode
}

/** Injects the ISpacesApi implementation consumed by the slice's hooks. */
export function SpacesApiProvider({ api, children }: SpacesApiProviderProps) {
  return <SpacesApiContext.Provider value={api}>{children}</SpacesApiContext.Provider>
}

export function useSpacesApi(): ISpacesApi {
  const api = useContext(SpacesApiContext)
  if (!api) {
    throw new Error('useSpacesApi must be used within a SpacesApiProvider')
  }
  return api
}
