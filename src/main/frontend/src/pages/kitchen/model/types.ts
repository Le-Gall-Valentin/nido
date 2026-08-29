export type RecipeCategory = 'PLAT' | 'EXPRESS' | 'VEGETARIAN' | 'DESSERT' | 'SOUP'

export type MeasurementUnit =
  | 'GRAM' | 'KILOGRAM' | 'MILLILITER' | 'CENTILITER' | 'LITER'
  | 'PIECE' | 'SLICE' | 'TABLESPOON' | 'TEASPOON' | 'PINCH' | 'SACHET'

export interface RecipeIngredient {
  name: string
  quantity: number
  unit: MeasurementUnit
}

export interface Recipe {
  id: string
  name: string
  description?: string | null
  category: RecipeCategory
  minutes: number
  referencePortions: number
  favorite: boolean
  ingredients: RecipeIngredient[]
  steps: string[]
  note?: string | null
  /** Only populated by the list endpoint; absent (undefined) on a single-recipe fetch. */
  lastPlannedOn?: string | null
}

export interface RecipeInput {
  name: string
  description?: string | null
  category: RecipeCategory
  minutes: number
  referencePortions: number
  ingredients: RecipeIngredient[]
  steps: string[]
  note?: string | null
}

export interface MenuEntry {
  id: string
  date: string
  recipeId: string
  recipeName: string
  recipeCategory: RecipeCategory
  portions: number
  position: number
}

export interface ShoppingListLine {
  name: string
  quantity: number
  unit: MeasurementUnit
}
