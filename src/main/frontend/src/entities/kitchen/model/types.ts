// Re-exported rather than redeclared: the same union lived here and in shared/lib, and two copies of
// a list of units drift the day one of them gains a unit. The shopping list reaches it from shared,
// so shared is where it belongs.
import type { MeasurementUnit } from '@/shared/lib'

export type { MeasurementUnit }

export type RecipeCategory = 'PLAT' | 'EXPRESS' | 'VEGETARIAN' | 'DESSERT' | 'SOUP'

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
