import { createContext, useContext, type ReactNode } from 'react'
import type { ISpaceMembersApi } from './ISpaceMembersApi'

const SpaceMembersApiContext = createContext<ISpaceMembersApi | null>(null)

interface SpaceMembersApiProviderProps {
  api: ISpaceMembersApi
  children: ReactNode
}

/** Injects the ISpaceMembersApi implementation consumed by useSpaceMembers. */
export function SpaceMembersApiProvider({ api, children }: SpaceMembersApiProviderProps) {
  return <SpaceMembersApiContext.Provider value={api}>{children}</SpaceMembersApiContext.Provider>
}

export function useSpaceMembersApi(): ISpaceMembersApi {
  const api = useContext(SpaceMembersApiContext)
  if (!api) {
    throw new Error('useSpaceMembersApi must be used within a SpaceMembersApiProvider')
  }
  return api
}
