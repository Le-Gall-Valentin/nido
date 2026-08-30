import type { MenuEntry, Recipe, RecipeInput, ShoppingListLine } from './types'

/**
 * Port for the Kitchen page (recipe book + weekly menu). Consumers (hooks)
 * depend on this contract, never on the concrete axios-backed
 * implementation, which is injected through KitchenApiProvider.
 */
export interface IKitchenApi {
  listRecipes(spaceId: string): Promise<Recipe[]>
  getRecipe(spaceId: string, recipeId: string): Promise<Recipe>
  createRecipe(spaceId: string, input: RecipeInput): Promise<Recipe>
  updateRecipe(spaceId: string, recipeId: string, input: RecipeInput): Promise<Recipe>
  deleteRecipe(spaceId: string, recipeId: string): Promise<void>
  toggleFavorite(spaceId: string, recipeId: string): Promise<Recipe>
  copyRecipe(spaceId: string, recipeId: string, destinationSpaceId: string): Promise<Recipe>
  moveRecipe(spaceId: string, recipeId: string, destinationSpaceId: string): Promise<Recipe>

  listMenuEntries(spaceId: string, from: string, to: string): Promise<MenuEntry[]>
  addMenuEntry(spaceId: string, date: string, recipeId: string, portions: number): Promise<MenuEntry>
  updateMenuEntryPortions(spaceId: string, entryId: string, portions: number): Promise<void>
  removeMenuEntry(spaceId: string, entryId: string): Promise<void>
  getShoppingList(spaceId: string, from: string, to: string): Promise<ShoppingListLine[]>
}
