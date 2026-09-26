import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { useCreateIntent, withCreateIntent } from './useCreateIntent'

function Probe({ enabled, open }: { enabled: boolean; open: () => void }) {
  useCreateIntent('item', enabled, open)
  return <p data-testid="search">{useLocation().search}</p>
}

function renderAt(url: string, enabled: boolean, open: () => void) {
  const tree = (on: boolean) => (
    <MemoryRouter initialEntries={[url]}>
      <Routes><Route path="/list" element={<Probe enabled={on} open={open} />} /></Routes>
    </MemoryRouter>
  )
  const view = render(tree(enabled))
  return { rerenderEnabled: () => view.rerender(tree(true)) }
}

describe('useCreateIntent', () => {
  it('opens the form once and takes the request out of the URL, keeping the other parameters', async () => {
    const open = vi.fn()
    renderAt('/list?view=week&create=item', true, open)

    await vi.waitFor(() => expect(screen.getByTestId('search').textContent).toBe('?view=week'))
    expect(open).toHaveBeenCalledTimes(1)
  })

  it('ignores a request meant for another page', () => {
    const open = vi.fn()
    renderAt('/list?create=event', true, open)

    expect(open).not.toHaveBeenCalled()
    expect(screen.getByTestId('search').textContent).toBe('?create=event')
  })

  it('holds the request until the caller may write — the role resolves after the first render', async () => {
    const open = vi.fn()
    const { rerenderEnabled } = renderAt('/list?create=item', false, open)
    expect(open).not.toHaveBeenCalled()

    rerenderEnabled()

    await vi.waitFor(() => expect(open).toHaveBeenCalledTimes(1))
    expect(screen.getByTestId('search').textContent).toBe('')
  })

  it('builds the link a page answers to', () => {
    expect(withCreateIntent('/s/space-1/finance', 'transaction')).toBe('/s/space-1/finance?create=transaction')
  })
})
