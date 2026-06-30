import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { SearchInput } from './SearchInput'

function setup(value = '', clearLabel: string | undefined = 'Clear') {
  const onChange = vi.fn()
  const result = render(
    <SearchInput value={value} onChange={onChange} placeholder="Search…" clearLabel={clearLabel} />
  )
  return { ...result, onChange }
}

describe('SearchInput', () => {
  it('renders with the placeholder as accessible name', () => {
    const { getByRole } = setup()
    expect(getByRole('searchbox', { name: 'Search…' })).toBeDefined()
  })

  it('calls onChange with the typed value', () => {
    const { getByRole, onChange } = setup()
    fireEvent.change(getByRole('searchbox'), { target: { value: 'alice' } })
    expect(onChange).toHaveBeenCalledWith('alice')
  })

  it('shows no clear button when the value is empty', () => {
    const { queryByRole } = setup('')
    expect(queryByRole('button')).toBeNull()
  })

  it('clears the value when the clear button is clicked', () => {
    const { getByRole, onChange } = setup('alice')
    fireEvent.click(getByRole('button', { name: 'Clear' }))
    expect(onChange).toHaveBeenCalledWith('')
  })

  it('renders no clear button when clearLabel is not provided', () => {
    const { queryByRole } = render(
      <SearchInput value="alice" onChange={vi.fn()} placeholder="Search…" />
    )
    expect(queryByRole('button')).toBeNull()
  })
})
