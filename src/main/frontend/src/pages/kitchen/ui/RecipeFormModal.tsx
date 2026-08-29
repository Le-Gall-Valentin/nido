import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { X } from 'lucide-react'
import { Dialog, Button, Input, Textarea, CTA_BUTTON_STYLE } from '@/shared/ui'
import { RECIPE_CATEGORY_META, RECIPE_CATEGORY_ORDER } from '../lib/recipeCategoryMeta'
import { RECIPE_UNITS, RECIPE_UNIT_LABEL_KEY } from '../lib/recipeUnitMeta'
import type { MeasurementUnit, Recipe, RecipeInput, RecipeCategory } from '../model/types'

interface IngredientDraft {
  name: string
  quantity: string
  unit: MeasurementUnit
}

interface RecipeFormModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (input: RecipeInput) => void
  initialRecipe: Recipe | null
}

function draftFrom(recipe: Recipe | null): { name: string; category: RecipeCategory; minutes: string; referencePortions: string; ingredients: IngredientDraft[]; steps: string[] } {
  if (!recipe) {
    return { name: '', category: 'PLAT', minutes: '', referencePortions: '', ingredients: [{ name: '', quantity: '', unit: 'GRAM' }], steps: [] }
  }
  return {
    name: recipe.name, category: recipe.category, minutes: String(recipe.minutes), referencePortions: String(recipe.referencePortions),
    ingredients: recipe.ingredients.map((i) => ({ name: i.name, quantity: String(i.quantity), unit: i.unit })),
    steps: recipe.steps,
  }
}

export function RecipeFormModal({ open, onClose, onSubmit, initialRecipe }: RecipeFormModalProps) {
  const { t } = useTranslation('kitchen')
  const [draft, setDraft] = useState(() => draftFrom(initialRecipe))
  const [error, setError] = useState('')

  function updateIngredient(index: number, patch: Partial<IngredientDraft>) {
    setDraft((d) => ({ ...d, ingredients: d.ingredients.map((ing, i) => (i === index ? { ...ing, ...patch } : ing)) }))
  }

  function addIngredient() {
    setDraft((d) => ({ ...d, ingredients: [...d.ingredients, { name: '', quantity: '', unit: 'GRAM' }] }))
  }

  function removeIngredient(index: number) {
    setDraft((d) => ({ ...d, ingredients: d.ingredients.filter((_, i) => i !== index) }))
  }

  function updateStep(index: number, value: string) {
    setDraft((d) => ({ ...d, steps: d.steps.map((s, i) => (i === index ? value : s)) }))
  }

  function addStep() {
    setDraft((d) => ({ ...d, steps: [...d.steps, ''] }))
  }

  function removeStep(index: number) {
    setDraft((d) => ({ ...d, steps: d.steps.filter((_, i) => i !== index) }))
  }

  function handleSave() {
    if (!draft.name.trim()) {
      setError(t('form.name_required'))
      return
    }
    const ingredients = draft.ingredients
      .filter((i) => i.name.trim() && i.quantity.trim())
      .map((i) => ({ name: i.name.trim(), quantity: Number(i.quantity), unit: i.unit }))
    if (ingredients.length === 0) {
      setError(t('form.ingredient_required'))
      return
    }
    setError('')
    onSubmit({
      name: draft.name.trim(),
      category: draft.category,
      minutes: Number(draft.minutes) || 1,
      referencePortions: Number(draft.referencePortions) || 1,
      ingredients,
      steps: draft.steps.map((s) => s.trim()).filter(Boolean),
    })
  }

  return (
    <Dialog open={open} onClose={onClose} title={t(initialRecipe ? 'form.edit_title' : 'form.create_title')} maxWidth="max-w-lg">
      <div className="flex flex-col gap-4">
        <Input label={t('form.name_label')} value={draft.name} onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))} />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="recipe-category" className="text-[13px] font-semibold text-fg-1">{t('form.category_label')}</label>
          <select
            id="recipe-category"
            value={draft.category}
            onChange={(e) => setDraft((d) => ({ ...d, category: e.target.value as RecipeCategory }))}
            className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none"
          >
            {RECIPE_CATEGORY_ORDER.map((c) => (
              <option key={c} value={c}>{t(RECIPE_CATEGORY_META[c].labelKey)}</option>
            ))}
          </select>
        </div>

        <div className="flex gap-3">
          <Input label={t('form.minutes_label')} type="number" min={1} value={draft.minutes}
            onChange={(e) => setDraft((d) => ({ ...d, minutes: e.target.value }))} />
          <Input label={t('form.portions_label')} type="number" min={1} value={draft.referencePortions}
            onChange={(e) => setDraft((d) => ({ ...d, referencePortions: e.target.value }))} />
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-[13px] font-semibold text-fg-1">{t('form.ingredients_title')}</span>
          {draft.ingredients.map((ing, index) => (
            <div key={index} className="flex items-center gap-2">
              <Input label={t('form.ingredient_name_label')} srOnlyLabel placeholder={t('form.ingredient_name_placeholder')}
                value={ing.name} onChange={(e) => updateIngredient(index, { name: e.target.value })} />
              <Input label={t('form.ingredient_quantity_label')} srOnlyLabel type="number" placeholder="0"
                value={ing.quantity} className="w-24" onChange={(e) => updateIngredient(index, { quantity: e.target.value })} />
              <select
                aria-label={t('form.ingredient_unit_label')}
                value={ing.unit}
                onChange={(e) => updateIngredient(index, { unit: e.target.value as MeasurementUnit })}
                className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-2 py-[11px] text-[13px] text-fg-0"
              >
                {RECIPE_UNITS.map((u) => <option key={u} value={u}>{t(RECIPE_UNIT_LABEL_KEY[u])}</option>)}
              </select>
              <button type="button" onClick={() => removeIngredient(index)} aria-label={t('form.remove')} className="p-2 text-fg-3 hover:text-status-red">
                <X className="size-4" />
              </button>
            </div>
          ))}
          <Button type="button" onClick={addIngredient} className="self-start">{t('form.add_ingredient')}</Button>
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-[13px] font-semibold text-fg-1">{t('form.steps_title')}</span>
          {draft.steps.map((step, index) => (
            <div key={index} className="flex items-start gap-2">
              <span className="mt-[9px] grid size-6 shrink-0 place-items-center rounded-full bg-bg-2 text-[11.5px] font-bold text-fg-3">
                {index + 1}
              </span>
              <Textarea label={t('form.step_label', { number: index + 1 })} value={step} rows={2}
                placeholder={t('form.step_placeholder')}
                onChange={(e) => updateStep(index, e.target.value)} />
              <button type="button" onClick={() => removeStep(index)} aria-label={t('form.remove')} className="mt-[9px] p-2 text-fg-3 hover:text-status-red">
                <X className="size-4" />
              </button>
            </div>
          ))}
          <Button type="button" onClick={addStep} className="self-start">{t('form.add_step')}</Button>
        </div>

        {error && <p className="text-sm font-medium text-status-red">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onClose}>{t('form.cancel')}</Button>
          <Button type="button" onClick={handleSave} style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </div>
    </Dialog>
  )
}
