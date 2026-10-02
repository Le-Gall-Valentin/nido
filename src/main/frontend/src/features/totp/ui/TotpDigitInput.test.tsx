import { render, fireEvent } from '@testing-library/react'
import { createRef } from 'react'
import { describe, it, expect, vi } from 'vitest'
import { TotpDigitInput, type TotpDigitInputHandle } from './TotpDigitInput'

function setup(value = '', onChange = vi.fn()) {
  const utils = render(<TotpDigitInput value={value} onChange={onChange} label="Code de vérification" />)
  const field = utils.getByRole('textbox', { name: 'Code de vérification' }) as HTMLInputElement
  const boxes = Array.from(utils.container.querySelector('[aria-hidden="true"]')!.children)
  return { ...utils, field, boxes }
}

describe('TotpDigitInput', () => {
  it('is a single one-time-code field, the one password managers and phones fill', () => {
    const { getAllByRole, field } = setup()
    expect(getAllByRole('textbox')).toHaveLength(1)
    expect(field.autocomplete).toBe('one-time-code')
  })

  it('takes a whole code written into it at once, as a password manager does', () => {
    // What Bitwarden's content script does: set the value through the native setter, then fire
    // input and change. Six one-character fields kept only the last digit, in the first box.
    const onChange = vi.fn()
    const { field } = setup('', onChange)
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(field, '123456')
    field.dispatchEvent(new Event('input', { bubbles: true }))
    field.dispatchEvent(new Event('change', { bubbles: true }))
    expect(onChange).toHaveBeenCalledWith('123456')
  })

  it('draws the value one digit per box', () => {
    const { boxes } = setup('123')
    expect(boxes.map(b => b.textContent)).toEqual(['1', '2', '3', '', '', ''])
  })

  it('keeps digits only, six at most', () => {
    const onChange = vi.fn()
    const { field } = setup('', onChange)
    fireEvent.change(field, { target: { value: '12a 34-5678' } })
    expect(onChange).toHaveBeenCalledWith('123456')
  })

  it('a paste replaces the code already typed, spaces and dashes dropped', () => {
    const onChange = vi.fn()
    const { field } = setup('1', onChange)
    fireEvent.paste(field, { clipboardData: { getData: () => '987 654' } })
    expect(onChange).toHaveBeenCalledWith('987654')
  })

  it('highlights the box the next digit goes in, only while the field has focus', () => {
    const { field, boxes } = setup('12')
    const highlighted = () => boxes.map(b => b.className.includes('border-accent'))
    expect(highlighted()).toEqual([false, false, false, false, false, false])
    fireEvent.focus(field)
    expect(highlighted()).toEqual([false, false, true, false, false, false])
    fireEvent.blur(field)
    expect(highlighted()).toEqual([false, false, false, false, false, false])
  })

  it('highlights the last box once the code is complete', () => {
    const { field, boxes } = setup('123456')
    fireEvent.focus(field)
    expect(boxes.map(b => b.className.includes('border-accent'))).toEqual([false, false, false, false, false, true])
  })

  it('keeps the caret after the last digit, where the highlighted box says the next one goes', () => {
    const { field } = setup('1234')
    field.setSelectionRange(1, 1)
    fireEvent.select(field)
    expect([field.selectionStart, field.selectionEnd]).toEqual([4, 4])
  })

  it('is disabled when asked', () => {
    const { getByRole } = render(<TotpDigitInput value="" onChange={vi.fn()} disabled />)
    expect((getByRole('textbox') as HTMLInputElement).disabled).toBe(true)
  })

  it('focus() through the ref focuses the field', () => {
    const ref = createRef<TotpDigitInputHandle>()
    const { getByRole } = render(<TotpDigitInput value="" onChange={vi.fn()} ref={ref} />)
    ref.current!.focus()
    expect(document.activeElement).toBe(getByRole('textbox'))
  })
})
