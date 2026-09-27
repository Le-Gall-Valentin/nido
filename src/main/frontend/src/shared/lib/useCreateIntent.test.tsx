import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { useCreateIntent, withCreateIntent } from './useCreateIntent'

function Probe({ canWrite, open }: { canWrite: boolean | undefined; open: () => void }) {
  useCreateIntent('item', canWrite, open)
  return <p data-testid="search">{useLocation().search}</p>
}

function renderAt(url: string, canWrite: boolean | undefined, open: () => void) {
  const tree = (rights: boolean | undefined) => (
    <MemoryRouter initialEntries={[url]}>
      <Routes><Route path="/list" element={<Probe canWrite={rights} open={open} />} /></Routes>
    </MemoryRouter>
  )
  const view = render(tree(canWrite))
  return { rerenderAllowed: () => view.rerender(tree(true)) }
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

  it("holds the request until the caller's rights are known — the role resolves after the first render", async () => {
    const open = vi.fn()
    const { rerenderAllowed } = renderAt('/list?create=item', undefined, open)
    expect(open).not.toHaveBeenCalled()
    expect(screen.getByTestId('search').textContent).toBe('?create=item')

    rerenderAllowed()

    await vi.waitFor(() => expect(open).toHaveBeenCalledTimes(1))
    expect(screen.getByTestId('search').textContent).toBe('')
  })

  it('drops a request the caller may not act on, without opening anything', async () => {
    // A viewer following an "add" link: nothing opens, and the request does not linger in the URL.
    const open = vi.fn()
    renderAt('/list?view=week&create=item', false, open)

    await vi.waitFor(() => expect(screen.getByTestId('search').textContent).toBe('?view=week'))
    expect(open).not.toHaveBeenCalled()
  })

  it('builds the link a page answers to', () => {
    expect(withCreateIntent('/s/space-1/finance', 'transaction')).toBe('/s/space-1/finance?create=transaction')
  })
})
