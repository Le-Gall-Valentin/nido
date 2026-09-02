import type { MeasurementUnit } from '@/shared/lib'

export interface ShoppingCategory {
  id: string
  name: string
  position: number
  fallback: boolean
}

export interface ShoppingItem {
  id: string
  categoryId: string
  name: string
  quantity?: number | null
  unit?: MeasurementUnit | null
  done: boolean
  position: number
}

export interface ShoppingImportLine {
  name: string
  quantity?: number | null
  unit?: MeasurementUnit | null
  categoryId: string
}
