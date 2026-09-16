import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Trash2 } from 'lucide-react'
import { Alert, Dialog, Button, Input, Spinner, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useRecipes, useMenuEntries, useAddMenuEntry, useRemoveMenuEntry } from '@/entities/kitchen'

interface MealEntryModalProps {
  spaceId: string
  /** The day being planned. */
  date: string
  onClose: () => void
}

/**
 * Planning a meal from the calendar.
 *
 * Owned by this page rather than shared with the kitchen: KitchenMenuPage plans a whole week
 * inline and has no modal to reuse, and "add a meal to this day" is a different gesture from
 * laying out a week. The duplication is three hook calls.
 */
export function MealEntryModal({ spaceId, date, onClose }: MealEntryModalProps) {
  const { t } = useTranslation('calendar')
  const { data: recipes, isPending: recipesPending } = useRecipes(spaceId)
  const { data: entries } = useMenuEntries(spaceId, date, date)
  const addEntry = useAddMenuEntry(spaceId)
  const removeEntry = useRemoveMenuEntry(spaceId)

  const [recipeId, setRecipeId] = useState('')
  const [portions, setPortions] = useState('4')
  const [error, setError] = useState<string | null>(null)

  const submit = () => {
    if (!recipeId) return setError(t('meal.recipe_required'))
    const count = Number(portions)
    if (!Number.isInteger(count) || count < 1) return setError(t('meal.portions_invalid'))
    setError(null)
    addEntry.mutate({ date, recipeId, portions: count }, {
      onSuccess: () => { setRecipeId(''); },
      onError: () => setError(t('meal.save_failed')),
    })
  }

  return (
    <Dialog open onClose={onClose} title={t('meal.title', { date })} showCloseButton>
      {recipesPending ? (
        <div className="flex justify-center py-6"><Spinner /></div>
      ) : (
        <div className="flex flex-col gap-3">
          {(entries ?? []).length > 0 && (
            <ul className="flex flex-col gap-1">
              {(entries ?? []).map((entry) => (
                <li key={entry.id} className="flex items-center gap-2 rounded-lg bg-bg-2 px-2 py-1.5">
                  <span className="flex-1 truncate text-sm text-fg-1">{entry.recipeName}</span>
                  <span className="text-xs text-fg-3">{t('meal.portions_short', { count: entry.portions })}</span>
                  <button type="button" aria-label={t('meal.remove', { name: entry.recipeName })}
                    onClick={() => removeEntry.mutate(entry.id)}
                    className="grid size-7 place-items-center rounded-md text-fg-3 hover:text-status-red">
                    <Trash2 className="size-4" />
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="flex flex-col gap-2">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-semibold text-fg-2">{t('meal.recipe')}</span>
              <select value={recipeId} onChange={(e) => setRecipeId(e.target.value)}
                className="rounded-lg border border-border bg-bg-1 px-3 py-2 text-sm text-fg-1">
                <option value="">{t('meal.pick_recipe')}</option>
                {(recipes ?? []).map((recipe) => (
                  <option key={recipe.id} value={recipe.id}>{recipe.name}</option>
                ))}
              </select>
            </label>
            <Input label={t('meal.portions')} type="number" min={1} value={portions}
              onChange={(e) => setPortions(e.target.value)} />
          </div>

          {error && <Alert variant="error">{error}</Alert>}

          <div className="flex justify-end gap-2">
            <Button type="button" onClick={onClose}>{t('form.cancel')}</Button>
            <Button type="button" style={CTA_BUTTON_STYLE} disabled={addEntry.isPending} onClick={submit}>
              {t('meal.add')}
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  )
}
