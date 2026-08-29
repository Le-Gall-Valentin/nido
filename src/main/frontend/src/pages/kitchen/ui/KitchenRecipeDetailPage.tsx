import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft } from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { ROUTES } from '@/shared/config'
import { kitchenApi } from '../api/kitchenApi'
import type { IKitchenApi } from '../model/IKitchenApi'
import { KitchenApiProvider } from '../model/kitchenApiContext'
import { useRecipe } from '../model/useRecipe'
import { RECIPE_CATEGORY_META } from '../lib/recipeCategoryMeta'

interface KitchenRecipeDetailPageProps {
  api?: IKitchenApi
}

export function KitchenRecipeDetailPage({ api = kitchenApi }: KitchenRecipeDetailPageProps = {}) {
  return (
    <KitchenApiProvider api={api}>
      <KitchenRecipeDetailPageContent />
    </KitchenApiProvider>
  )
}

function KitchenRecipeDetailPageContent() {
  const { t } = useTranslation('kitchen')
  const { spaceId = '', recipeId } = useParams<{ spaceId: string; recipeId: string }>()
  const { data: recipe, isPending, isError } = useRecipe(spaceId, recipeId)

  if (isPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError || !recipe) return <Alert variant="error">{t('error.load_failed')}</Alert>

  const meta = RECIPE_CATEGORY_META[recipe.category]

  return (
    <div className="mx-auto max-w-[720px] px-5 py-6 md:px-10 md:py-[34px]">
      <Link to={ROUTES.spaceKitchenRecipes(spaceId)} className="mb-5 flex items-center gap-1.5 text-sm font-semibold text-fg-3 hover:text-fg-1">
        <ArrowLeft className="size-4" /> {t('detail.back_to_recipes')}
      </Link>

      <h1 className="text-[27px] font-bold text-fg-0">{recipe.name}</h1>
      <p className="mt-1 text-sm text-fg-3">{t(meta.labelKey)} · {recipe.minutes} min · {recipe.referencePortions} {t('detail.portions_label')}</p>

      <div className="mt-6 grid gap-5 md:grid-cols-2">
        <div className="rounded-2xl border border-border bg-bg-1 p-5">
          <h2 className="mb-3 text-base font-semibold text-fg-0">{t('detail.ingredients_title')}</h2>
          <ul className="flex flex-col gap-2">
            {recipe.ingredients.map((ing, index) => (
              <li key={index} className="flex justify-between border-b border-border py-1.5 text-sm last:border-b-0">
                <span className="text-fg-2">{ing.name}</span>
                <span className="font-semibold text-fg-0">{ing.quantity} {ing.unit}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="rounded-2xl border border-border bg-bg-1 p-5">
          <h2 className="mb-3 text-base font-semibold text-fg-0">{t('detail.steps_title')}</h2>
          {recipe.steps.length === 0 ? (
            <p className="text-sm text-fg-3">{t('detail.no_steps')}</p>
          ) : (
            <ol className="flex flex-col gap-3">
              {recipe.steps.map((step, index) => (
                <li key={index} className="flex gap-3 text-sm text-fg-2">
                  <span className="grid size-5 shrink-0 place-items-center rounded-full bg-accent-dim text-xs font-bold text-accent">{index + 1}</span>
                  {step}
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </div>
  )
}
