import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Pagination } from './Pagination'

function setup(overrides: Partial<React.ComponentProps<typeof Pagination>> = {}) {
  const onPageChange = vi.fn()
  const result = render(
    <Pagination
      page={1}
      totalPages={3}
      onPageChange={onPageChange}
      pageLabel="Page 2 of 3"
      prevLabel="Previous"
      nextLabel="Next"
      summary="42 users"
      {...overrides}
    />
  )
  return { ...result, onPageChange }
}

describe('Pagination', () => {
  it('renders summary and page label', () => {
    const { getByText } = setup()
    expect(getByText('42 users')).toBeDefined()
    expect(getByText('Page 2 of 3')).toBeDefined()
  })

  it('calls onPageChange with page-1 on previous click', () => {
    const { getByRole, onPageChange } = setup()
    fireEvent.click(getByRole('button', { name: 'Previous' }))
    expect(onPageChange).toHaveBeenCalledWith(0)
  })

  it('calls onPageChange with page+1 on next click', () => {
    const { getByRole, onPageChange } = setup()
    fireEvent.click(getByRole('button', { name: 'Next' }))
    expect(onPageChange).toHaveBeenCalledWith(2)
  })

  it('disables previous on first page', () => {
    const { getByRole } = setup({ page: 0 })
    expect((getByRole('button', { name: 'Previous' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables next on last page', () => {
    const { getByRole } = setup({ page: 2 })
    expect((getByRole('button', { name: 'Next' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('dims controls while transitioning', () => {
    const { container } = setup({ isTransitioning: true })
    expect(container.querySelector('nav')?.className).toContain('pointer-events-none')
  })
})
