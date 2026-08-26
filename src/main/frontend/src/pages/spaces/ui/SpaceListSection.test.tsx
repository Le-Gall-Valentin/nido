import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { SpaceSummary } from '@/entities/space'
import { SpaceListSection } from './SpaceListSection'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const PERSONAL: SpaceSummary = { id: 'p-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1 }
const SHARED: SpaceSummary = { id: 's-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'ADMIN', memberCount: 3 }

describe('SpaceListSection — personal space', () => {
  it('renders the personal space first and not as a clickable button', () => {
    render(<SpaceListSection spaces={[SHARED, PERSONAL]} onSelect={vi.fn()} onCreateClick={vi.fn()} />)
    const items = screen.getAllByRole('listitem')
    expect(items[0].textContent).toContain('Alice')
    expect(screen.queryByRole('button', { name: /Alice/ })).toBeNull()
  })
})

describe('SpaceListSection — shared groups', () => {
  it('renders a clickable row per shared group with member count and role', () => {
    render(<SpaceListSection spaces={[PERSONAL, SHARED]} onSelect={vi.fn()} onCreateClick={vi.fn()} />)
    const button = screen.getByRole('button', { name: /Chez nous/ })
    expect(button.textContent).toContain('Chez nous')
  })

  it('calls onSelect with the group id when clicked', () => {
    const onSelect = vi.fn()
    render(<SpaceListSection spaces={[PERSONAL, SHARED]} onSelect={onSelect} onCreateClick={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: /Chez nous/ }))
    expect(onSelect).toHaveBeenCalledWith('s-1')
  })

  it('shows the empty message when there is no shared group', () => {
    render(<SpaceListSection spaces={[PERSONAL]} onSelect={vi.fn()} onCreateClick={vi.fn()} />)
    expect(screen.getByText('list.empty')).toBeDefined()
  })
})

describe('SpaceListSection — create action', () => {
  it('calls onCreateClick when the create button is clicked', () => {
    const onCreateClick = vi.fn()
    render(<SpaceListSection spaces={[PERSONAL]} onSelect={vi.fn()} onCreateClick={onCreateClick} />)
    fireEvent.click(screen.getByText('list.action_create'))
    expect(onCreateClick).toHaveBeenCalledOnce()
  })
})
