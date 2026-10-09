import { createContext, useContext } from 'react'
import type { ISpaceMembersApi } from './ISpaceMembersApi'

export const SpaceMembersApiContext = createContext<ISpaceMembersApi | null>(null)

export function useSpaceMembersApi(): ISpaceMembersApi {
  const api = useContext(SpaceMembersApiContext)
  if (!api) {
    throw new Error('useSpaceMembersApi must be used within a SpaceMembersApiProvider')
  }
  return api
}
