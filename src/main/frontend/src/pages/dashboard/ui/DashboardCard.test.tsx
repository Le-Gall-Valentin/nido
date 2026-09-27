import { screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Wallet } from 'lucide-react'
import { DashboardCard } from './DashboardCard'
import { renderWithActions } from '../test/renderWithActions'

describe('DashboardCard', () => {
  it('is a region named by its title, with its "see all" link and its footer', () => {
    renderWithActions(
      <DashboardCard icon={Wallet} title="Finances" link={{ to: '/s/space-1/finance', label: 'Détails' }} footer={<p>pied</p>}>
        <p>corps</p>
      </DashboardCard>
    )

    expect(screen.getByRole('region', { name: 'Finances' })).toBeDefined()
    expect(screen.getByRole('link', { name: 'Détails' }).getAttribute('href')).toBe('/s/space-1/finance')
    expect(screen.getByText('corps')).toBeDefined()
    expect(screen.getByText('pied')).toBeDefined()
  })
})
