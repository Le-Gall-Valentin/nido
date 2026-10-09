import type { ReactNode } from 'react'
import type { ISpacesApi } from './ISpacesApi'
import { SpacesApiContext } from './spacesApiContext'

interface SpacesApiProviderProps {
  api: ISpacesApi
  children: ReactNode
}

/** Injects the ISpacesApi implementation consumed by the slice's hooks. */
export function SpacesApiProvider({ api, children }: SpacesApiProviderProps) {
  return <SpacesApiContext.Provider value={api}>{children}</SpacesApiContext.Provider>
}
