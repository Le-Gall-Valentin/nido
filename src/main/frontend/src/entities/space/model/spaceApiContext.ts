import { createContext, useContext } from 'react'
import type { ISpaceApi } from './ISpaceApi'

export const SpaceApiContext = createContext<ISpaceApi | null>(null)

export function useSpaceApi(): ISpaceApi {
  const api = useContext(SpaceApiContext)
  if (!api) {
    throw new Error('useSpaceApi must be used within a SpaceApiProvider')
  }
  return api
}
