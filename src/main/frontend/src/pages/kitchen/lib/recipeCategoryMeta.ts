import type { RecipeCategory } from '../model/types'

export interface RecipeCategoryMeta {
  labelKey: string
  color: string
}

/** Colors/labels are a frontend-only concern — the backend stores only the category code. */
export const RECIPE_CATEGORY_META: Record<RecipeCategory, RecipeCategoryMeta> = {
  PLAT: { labelKey: 'category.plat', color: '#c17a5c' },
  EXPRESS: { labelKey: 'category.express', color: '#4a7fa0' },
  VEGETARIAN: { labelKey: 'category.vegetarian', color: '#5c7a58' },
  DESSERT: { labelKey: 'category.dessert', color: '#c98aa6' },
  SOUP: { labelKey: 'category.soup', color: '#7a6f9c' },
}

export const RECIPE_CATEGORY_ORDER: RecipeCategory[] = ['PLAT', 'EXPRESS', 'VEGETARIAN', 'DESSERT', 'SOUP']

export const RECIPE_UNITS = [
  'GRAM', 'KILOGRAM', 'MILLILITER', 'CENTILITER', 'LITER',
  'PIECE', 'SLICE', 'TABLESPOON', 'TEASPOON', 'PINCH', 'SACHET',
] as const
