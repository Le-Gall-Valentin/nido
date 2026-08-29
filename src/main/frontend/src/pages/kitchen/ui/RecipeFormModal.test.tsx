import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { RecipeFormModal } from './RecipeFormModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

describe('RecipeFormModal — create', () => {
  it('submits the entered fields as a RecipeInput', () => {
    const onSubmit = vi.fn()
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialRecipe={null} />)

    fireEvent.change(screen.getByLabelText('form.name_label'), { target: { value: 'Riz cantonais' } })
    fireEvent.change(screen.getByLabelText('form.minutes_label'), { target: { value: '20' } })
    fireEvent.change(screen.getByLabelText('form.portions_label'), { target: { value: '2' } })
    fireEvent.change(screen.getAllByLabelText('form.ingredient_name_label')[0], { target: { value: 'Riz' } })
    fireEvent.change(screen.getAllByLabelText('form.ingredient_quantity_label')[0], { target: { value: '200' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'Riz cantonais', category: 'PLAT', minutes: 20, referencePortions: 2,
      ingredients: [{ name: 'Riz', quantity: 200, unit: 'GRAM' }], steps: [],
    })
  })

  it('rejects a blank name', () => {
    const onSubmit = vi.fn()
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialRecipe={null} />)

    fireEvent.change(screen.getAllByLabelText('form.ingredient_name_label')[0], { target: { value: 'Riz' } })
    fireEvent.change(screen.getAllByLabelText('form.ingredient_quantity_label')[0], { target: { value: '200' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).not.toHaveBeenCalled()
    expect(screen.getByText('form.name_required')).toBeDefined()
  })

  it('adds another ingredient row', () => {
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialRecipe={null} />)

    fireEvent.click(screen.getByText('form.add_ingredient'))

    expect(screen.getAllByLabelText('form.ingredient_name_label')).toHaveLength(2)
  })

  it('shows translated unit labels instead of the raw enum', () => {
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialRecipe={null} />)

    const options = screen.getAllByLabelText<HTMLSelectElement>('form.ingredient_unit_label')[0].querySelectorAll('option')

    expect(options[0]).toHaveProperty('textContent', 'unit.GRAM')
    expect(Array.from(options).some((o) => o.textContent === 'GRAM')).toBe(false)
  })

  it('uses a resizable, multi-line textarea for each step', () => {
    const onSubmit = vi.fn()
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={onSubmit} initialRecipe={null} />)

    fireEvent.change(screen.getByLabelText('form.name_label'), { target: { value: 'Riz cantonais' } })
    fireEvent.change(screen.getAllByLabelText('form.ingredient_name_label')[0], { target: { value: 'Riz' } })
    fireEvent.change(screen.getAllByLabelText('form.ingredient_quantity_label')[0], { target: { value: '200' } })
    fireEvent.click(screen.getByText('form.add_step'))

    const step = screen.getByLabelText<HTMLTextAreaElement>('form.step_label')
    expect(step.tagName).toBe('TEXTAREA')
    expect(step.rows).toBe(2)
    expect(step.className).toContain('resize-y')

    fireEvent.change(step, { target: { value: "Faire revenir l'oignon." } })
    fireEvent.click(screen.getByText('form.save'))

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ steps: ["Faire revenir l'oignon."] }))
  })
})

describe('RecipeFormModal — edit', () => {
  it('pre-fills the fields from the given recipe', () => {
    render(<RecipeFormModal open onClose={vi.fn()} onSubmit={vi.fn()} initialRecipe={{
      id: 'r1', name: 'Riz cantonais', category: 'PLAT', minutes: 20, referencePortions: 2,
      favorite: false, ingredients: [{ name: 'Riz', quantity: 200, unit: 'GRAM' }], steps: [],
    }} />)

    expect(screen.getByLabelText('form.name_label')).toHaveProperty('value', 'Riz cantonais')
  })
})
