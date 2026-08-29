import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, Trash2 } from 'lucide-react'
import { Alert, Spinner } from '@/shared/ui'
import { kitchenApi } from '../api/kitchenApi'
import type { IKitchenApi } from '../model/IKitchenApi'
import { KitchenApiProvider } from '../model/kitchenApiContext'
import { useRecipes } from '../model/useRecipes'
import { useMenuEntries } from '../model/useMenuEntries'
import { useShoppingList } from '../model/useShoppingList'
import { useAddMenuEntry, useRemoveMenuEntry, useUpdateMenuEntryPortions } from '../model/useMenuMutations'
import { startOfWeek, addDays, toISODate, weekDates } from '../lib/weekRange'
import type { MenuEntry, Recipe } from '../model/types'

interface KitchenMenuPageProps {
  api?: IKitchenApi
  /** Testability seam: defaults to the real current week; tests pin a fixed date instead. */
  initialWeekStart?: Date
}

export function KitchenMenuPage({ api = kitchenApi, initialWeekStart }: KitchenMenuPageProps = {}) {
  return (
    <KitchenApiProvider api={api}>
      <KitchenMenuPageContent initialWeekStart={initialWeekStart} />
    </KitchenApiProvider>
  )
}

/** Oldest/never-planned first — this is the "suggestions" feature: no dedicated endpoint, just this sort. */
function sortByLastPlanned(recipes: Recipe[]): Recipe[] {
  return [...recipes].sort((a, b) => {
    if (!a.lastPlannedOn && !b.lastPlannedOn) return a.name.localeCompare(b.name)
    if (!a.lastPlannedOn) return -1
    if (!b.lastPlannedOn) return 1
    return a.lastPlannedOn.localeCompare(b.lastPlannedOn)
  })
}

function KitchenMenuPageContent({ initialWeekStart }: { initialWeekStart?: Date }) {
  const { t } = useTranslation('kitchen')
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  const [weekStart, setWeekStart] = useState(() => startOfWeek(initialWeekStart ?? new Date()))
  const days = useMemo(() => weekDates(weekStart), [weekStart])
  const from = toISODate(days[0])
  const to = toISODate(days[6])

  const { data: recipes } = useRecipes(spaceId)
  const { data: entries, isPending, isError } = useMenuEntries(spaceId, from, to)
  const { data: shoppingList } = useShoppingList(spaceId, from, to)
  const addEntry = useAddMenuEntry(spaceId)
  const removeEntry = useRemoveMenuEntry(spaceId)
  const updatePortions = useUpdateMenuEntryPortions(spaceId)

  const sortedRecipes = useMemo(() => sortByLastPlanned(recipes ?? []), [recipes])
  const entriesByDate = useMemo(() => {
    const map = new Map<string, MenuEntry[]>()
    for (const entry of entries ?? []) {
      map.set(entry.date, [...(map.get(entry.date) ?? []), entry])
    }
    return map
  }, [entries])

  const [pickerDate, setPickerDate] = useState<string | null>(null)
  const [pickerRecipeId, setPickerRecipeId] = useState('')
  const [pickerPortions, setPickerPortions] = useState('4')

  function openPicker(date: string) {
    const defaultRecipe = sortedRecipes[0]
    setPickerDate(date)
    setPickerRecipeId(defaultRecipe?.id ?? '')
    setPickerPortions(String(defaultRecipe?.referencePortions ?? 4))
  }

  function confirmAdd() {
    if (!pickerDate || !pickerRecipeId) return
    addEntry.mutate(
      { date: pickerDate, recipeId: pickerRecipeId, portions: Number(pickerPortions) || 1 },
      { onSuccess: () => setPickerDate(null) }
    )
  }

  if (isPending) return <Spinner label={t('loading')} fullscreen={false} />
  if (isError) return <Alert variant="error">{t('error.load_failed')}</Alert>

  return (
    <div className="mx-auto max-w-[1000px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-fg-0">{t('menu.title')}</h1>
        <div className="flex items-center gap-2">
          <button type="button" aria-label={t('menu.previous_week')} onClick={() => setWeekStart(addDays(weekStart, -7))}
            className="grid size-9 place-items-center rounded-[10px] border border-border hover:bg-bg-3">
            <ChevronLeft className="size-4" />
          </button>
          <span className="text-sm font-semibold text-fg-2">{from} – {to}</span>
          <button type="button" aria-label={t('menu.next_week')} onClick={() => setWeekStart(addDays(weekStart, 7))}
            className="grid size-9 place-items-center rounded-[10px] border border-border hover:bg-bg-3">
            <ChevronRight className="size-4" />
          </button>
        </div>
      </div>

      <div className="grid gap-5 md:grid-cols-[1.3fr_1fr]">
        <div className="rounded-2xl border border-border bg-bg-1 p-5">
          <div className="flex flex-col gap-4">
            {days.map((day) => {
              const date = toISODate(day)
              const dayEntries = entriesByDate.get(date) ?? []
              return (
                <div key={date}>
                  <div className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-fg-3">{date}</div>
                  <div className="flex flex-col gap-1.5">
                    {dayEntries.map((entry) => (
                      <div key={entry.id} className="flex items-center gap-2 rounded-[9px] border border-border px-3 py-2">
                        <span className="flex-1 truncate text-sm text-fg-1">{entry.recipeName}</span>
                        <input
                          type="number" min={1} defaultValue={entry.portions} aria-label={t('menu.portions_label')}
                          key={`${entry.id}-${entry.portions}`}
                          onBlur={(e) => updatePortions.mutate({ entryId: entry.id, portions: Number(e.target.value) || 1 })}
                          className="w-14 rounded-md border border-border bg-bg-1 px-1.5 py-1 text-center text-xs"
                        />
                        <button type="button" aria-label={t('menu.remove_meal')} onClick={() => removeEntry.mutate(entry.id)}
                          className="p-1 text-fg-3 hover:text-status-red">
                          <Trash2 className="size-3.5" />
                        </button>
                      </div>
                    ))}

                    {pickerDate === date ? (
                      <div className="flex items-center gap-2 rounded-[9px] border border-dashed border-border-2 px-3 py-2">
                        <select value={pickerRecipeId} onChange={(e) => setPickerRecipeId(e.target.value)}
                          className="flex-1 rounded-md border border-border bg-bg-1 px-2 py-1 text-xs">
                          {sortedRecipes.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                        </select>
                        <input type="number" min={1} value={pickerPortions} onChange={(e) => setPickerPortions(e.target.value)}
                          className="w-14 rounded-md border border-border bg-bg-1 px-1.5 py-1 text-center text-xs" />
                        <button type="button" onClick={confirmAdd} className="text-xs font-semibold text-accent">
                          {t('menu.confirm_add')}
                        </button>
                        <button type="button" onClick={() => setPickerDate(null)} className="text-xs text-fg-3">
                          {t('form.cancel')}
                        </button>
                      </div>
                    ) : (
                      <button type="button" onClick={() => openPicker(date)}
                        className="rounded-[9px] border border-dashed border-border-2 px-3 py-2 text-left text-xs font-semibold text-fg-3 hover:text-fg-1">
                        + {t('menu.add_meal')}
                      </button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-bg-1 p-5">
          <h2 className="mb-1 text-base font-semibold text-fg-0">{t('menu.shopping_list_title')}</h2>
          <p className="mb-4 text-xs text-fg-3">{t('menu.shopping_list_subtitle')}</p>
          {(shoppingList ?? []).length === 0 ? (
            <p className="text-sm text-fg-3">{t('menu.shopping_list_empty')}</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {(shoppingList ?? []).map((line, index) => (
                <li key={index} className="flex justify-between border-b border-border py-1.5 text-sm last:border-b-0">
                  <span className="text-fg-2">{line.name}</span>
                  <span className="font-semibold text-fg-0">{line.quantity} {line.unit}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
