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
  quantityLabel?: string | null
  done: boolean
  position: number
}

export interface ShoppingImportLine {
  name: string
  quantityLabel?: string | null
  categoryId: string
}
