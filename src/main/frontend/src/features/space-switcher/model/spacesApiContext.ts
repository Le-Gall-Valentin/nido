import { createContext, useContext } from 'react'
import type { ISpacesApi } from './ISpacesApi'

export const SpacesApiContext = createContext<ISpacesApi | null>(null)

export function useSpacesApi(): ISpacesApi {
  const api = useContext(SpacesApiContext)
  if (!api) {
    throw new Error('useSpacesApi must be used within a SpacesApiProvider')
  }
  return api
}
