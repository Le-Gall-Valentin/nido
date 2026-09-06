import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { IconPickerModal } from './IconPickerModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const ICON_NAMES = ['Home', 'Wallet', 'Car', 'Heart']

describe('IconPickerModal', () => {
  it('shows every provided icon when the search is empty', () => {
    render(<IconPickerModal initialIcon="Home" initialColor="#000000" iconNames={ICON_NAMES} onConfirm={vi.fn()} onCancel={vi.fn()} />)

    for (const name of ICON_NAMES) {
      expect(screen.getByRole('button', { name })).toBeDefined()
    }
  })

  it('filters the grid by search text', () => {
    render(<IconPickerModal initialIcon="Home" initialColor="#000000" iconNames={ICON_NAMES} onConfirm={vi.fn()} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('categories.icon_search_label'), { target: { value: 'wal' } })

    expect(screen.getByRole('button', { name: 'Wallet' })).toBeDefined()
    expect(screen.queryByRole('button', { name: 'Home' })).toBeNull()
  })

  it('confirms the selected icon and color', () => {
    const onConfirm = vi.fn()
    render(<IconPickerModal initialIcon="Home" initialColor="#000000" iconNames={ICON_NAMES} onConfirm={onConfirm} onCancel={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Wallet' }))
    fireEvent.click(screen.getByText('categories.icon_confirm'))

    expect(onConfirm).toHaveBeenCalledWith('Wallet', '#000000')
  })

  it('confirms a changed color together with the initial icon', () => {
    const onConfirm = vi.fn()
    render(<IconPickerModal initialIcon="Home" initialColor="#000000" iconNames={ICON_NAMES} onConfirm={onConfirm} onCancel={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('categories.color_label'), { target: { value: '#112233' } })
    fireEvent.click(screen.getByText('categories.icon_confirm'))

    expect(onConfirm).toHaveBeenCalledWith('Home', '#112233')
  })
})
