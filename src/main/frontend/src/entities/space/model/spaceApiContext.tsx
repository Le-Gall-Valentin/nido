import { createContext, useContext, type ReactNode } from 'react'
import type { ISpaceApi } from './ISpaceApi'

const SpaceApiContext = createContext<ISpaceApi | null>(null)

interface SpaceApiProviderProps {
  api: ISpaceApi
  children: ReactNode
}

/** Injects the ISpaceApi implementation consumed by this page's hooks. */
export function SpaceApiProvider({ api, children }: SpaceApiProviderProps) {
  return <SpaceApiContext.Provider value={api}>{children}</SpaceApiContext.Provider>
}

export function useSpaceApi(): ISpaceApi {
  const api = useContext(SpaceApiContext)
  if (!api) {
    throw new Error('useSpaceApi must be used within a SpaceApiProvider')
  }
  return api
}
