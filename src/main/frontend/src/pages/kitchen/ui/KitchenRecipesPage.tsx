import { useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Star, Clock, Users, Plus, Pencil } from 'lucide-react'
import { Alert, Button, SearchInput, Spinner, CTA_BUTTON_STYLE } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { kitchenApi } from '../api/kitchenApi'
import type { IKitchenApi } from '../model/IKitchenApi'
import { KitchenApiProvider } from '../model/kitchenApiContext'
import { useRecipes } from '../model/useRecipes'
import { useCreateRecipe, useDeleteRecipe, useToggleFavorite, useUpdateRecipe } from '../model/useRecipeMutations'
import { RECIPE_CATEGORY_META } from '../lib/recipeCategoryMeta'
import { RecipeFormModal } from './RecipeFormModal'
import { DeleteRecipeModal } from './DeleteRecipeModal'
import type { Recipe, RecipeInput } from '../model/types'

interface KitchenRecipesPageProps {
  api?: IKitchenApi
}

export function KitchenRecipesPage({ api = kitchenApi }: KitchenRecipesPageProps = {}) {
  return (
    <KitchenApiProvider api={api}>
      <KitchenRecipesPageContent />
    </KitchenApiProvider>
  )
}

function KitchenRecipesPageContent() {
  const { t } = useTranslation('kitchen')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const { data: recipes, isPending, isError } = useRecipes(spaceId)
  const createRecipe = useCreateRecipe(spaceId)
  const deleteRecipe = useDeleteRecipe(spaceId)
  const toggleFavorite = useToggleFavorite(spaceId)
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editingRecipe, setEditingRecipe] = useState<Recipe | null>(null)
  const [deletingRecipe, setDeletingRecipe] = useState<Recipe | null>(null)
  const updateRecipe = useUpdateRecipe(spaceId, editingRecipe?.id ?? '')

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return recipes ?? []
    return (recipes ?? []).filter((r) =>
      r.name.toLowerCase().includes(q) || r.ingredients.some((i) => i.name.toLowerCase().includes(q)))
  }, [recipes, search])

  function handleCreate(input: RecipeInput) {
    createRecipe.mutate(input, { onSuccess: () => setCreateOpen(false) })
  }

  function handleUpdate(input: RecipeInput) {
    updateRecipe.mutate(input, { onSuccess: () => setEditingRecipe(null) })
  }

  if (isPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  return (
    <div className="mx-auto max-w-[900px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('recipes.title')}</h1>
        <Button type="button" onClick={() => setCreateOpen(true)} style={CTA_BUTTON_STYLE}>
          <Plus className="size-4" /> {t('recipes.new_recipe')}
        </Button>
      </div>

      <SearchInput value={search} onChange={setSearch} placeholder={t('recipes.search_placeholder')}
        clearLabel={t('recipes.clear_search')} className="mb-5 max-w-sm" />

      {filtered.length === 0 ? (
        <p className="text-sm text-fg-3">{t('recipes.empty')}</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
          {filtered.map((recipe) => {
            const meta = RECIPE_CATEGORY_META[recipe.category]
            return (
              <div key={recipe.id} className="flex flex-col gap-3 rounded-2xl border border-border bg-bg-1 p-4">
                <div className="flex items-start gap-3">
                  <span className="grid size-9 shrink-0 place-items-center rounded-[10px] text-sm font-bold text-white" style={{ background: meta.color }}>
                    {recipe.name.charAt(0).toUpperCase()}
                  </span>
                  <div className="min-w-0 flex-1">
                    <Link to={ROUTES.spaceKitchenRecipe(spaceId, recipe.id)} className="block truncate text-[14.5px] font-semibold text-fg-0 hover:underline">
                      {recipe.name}
                    </Link>
                    <div className="mt-0.5 text-xs text-fg-3">{t(meta.labelKey)}</div>
                  </div>
                  <button type="button" onClick={() => toggleFavorite.mutate(recipe.id)}
                    aria-label={t(recipe.favorite ? 'unfavorite' : 'favorite')}>
                    <Star className={`size-4 ${recipe.favorite ? 'fill-amber-400 text-amber-400' : 'text-fg-3'}`} />
                  </button>
                </div>
                <div className="flex gap-4 text-xs text-fg-3">
                  <span className="flex items-center gap-1"><Clock className="size-3.5" /> {recipe.minutes} min</span>
                  <span className="flex items-center gap-1"><Users className="size-3.5" /> {recipe.referencePortions}</span>
                </div>
                <div className="flex gap-2 border-t border-border pt-2.5">
                  <button type="button" onClick={() => setEditingRecipe(recipe)}
                    className="flex flex-1 items-center justify-center gap-1 rounded-[9px] py-1.5 text-xs font-semibold text-fg-2 hover:bg-bg-2">
                    <Pencil className="size-3.5" /> {t('edit')}
                  </button>
                  <button type="button" onClick={() => setDeletingRecipe(recipe)}
                    className="flex-1 rounded-[9px] py-1.5 text-xs font-semibold text-status-red hover:bg-status-red-dim">
                    {t('delete')}
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <RecipeFormModal open={createOpen} onClose={() => setCreateOpen(false)} onSubmit={handleCreate} initialRecipe={null} />

      {editingRecipe && (
        <RecipeFormModal open onClose={() => setEditingRecipe(null)} onSubmit={handleUpdate} initialRecipe={editingRecipe} />
      )}

      {deletingRecipe && (
        <DeleteRecipeModal
          recipeName={deletingRecipe.name}
          onClose={() => setDeletingRecipe(null)}
          onDelete={() => deleteRecipe.mutateAsync(deletingRecipe.id)}
        />
      )}
    </div>
  )
}
