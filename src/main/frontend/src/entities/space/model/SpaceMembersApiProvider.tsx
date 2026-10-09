import type { ReactNode } from 'react'
import type { ISpaceMembersApi } from './ISpaceMembersApi'
import { SpaceMembersApiContext } from './spaceMembersApiContext'

interface SpaceMembersApiProviderProps {
  api: ISpaceMembersApi
  children: ReactNode
}

/** Injects the ISpaceMembersApi implementation consumed by useSpaceMembers. */
export function SpaceMembersApiProvider({ api, children }: SpaceMembersApiProviderProps) {
  return <SpaceMembersApiContext.Provider value={api}>{children}</SpaceMembersApiContext.Provider>
}
