import type React from 'react'
import { useEffect, useMemo } from 'react'
import { create } from 'zustand'

export type PaletteIconComponent = React.ComponentType<{ size?: number; className?: string }>

export interface PaletteItem {
  id: string
  label: string
  to?: string
  action?: () => void
  icon: PaletteIconComponent
  group?: string
}

interface PaletteRegistryState {
  sources: Record<string, PaletteItem[]>
  register: (sourceId: string, items: PaletteItem[]) => void
  unregister: (sourceId: string) => void
}

const paletteRegistryStore = create<PaletteRegistryState>((set) => ({
  sources: {},
  register: (sourceId, items) =>
    set((state) => {
      if (import.meta.env.DEV && sourceId in state.sources) {
        console.warn(`[paletteRegistry] sourceId "${sourceId}" already registered — previous items will be overwritten.`)
      }
      return { sources: { ...state.sources, [sourceId]: items } }
    }),
  unregister: (sourceId) =>
    set((state) => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { [sourceId]: _removed, ...rest } = state.sources
      return { sources: rest }
    }),
}))

/**
 * Registers palette items under a given source ID.
 * Items are removed when the component unmounts.
 * The `items` array must be stable (wrap with useMemo at the call site).
 */
export function usePaletteItems(sourceId: string, items: PaletteItem[]): void {
  const register = paletteRegistryStore((s) => s.register)
  const unregister = paletteRegistryStore((s) => s.unregister)

  useEffect(() => {
    register(sourceId, items)
    return () => unregister(sourceId)
  }, [sourceId, items, register, unregister])
}

/**
 * Returns all palette items from every registered source, in registration order.
 */
export function usePaletteResults(): PaletteItem[] {
  const sources = paletteRegistryStore((s) => s.sources)
  return useMemo(() => Object.values(sources).flat(), [sources])
}