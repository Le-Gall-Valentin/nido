import type { ReactNode } from 'react'
import type { ISpaceApi } from './ISpaceApi'
import { SpaceApiContext } from './spaceApiContext'

interface SpaceApiProviderProps {
  api: ISpaceApi
  children: ReactNode
}

/** Injects the ISpaceApi implementation consumed by this page's hooks. */
export function SpaceApiProvider({ api, children }: SpaceApiProviderProps) {
  return <SpaceApiContext.Provider value={api}>{children}</SpaceApiContext.Provider>
}
