import { render } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ProfileSummaryCard } from './ProfileSummaryCard'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k, i18n: { language: 'en' } }),
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

describe('ProfileSummaryCard', () => {
  it('shows username', () => {
    const { getByText } = render(<ProfileSummaryCard user={BASE_USER} />)
    expect(getByText('alice')).toBeDefined()
  })

  it('shows email', () => {
    const { getByText } = render(<ProfileSummaryCard user={BASE_USER} />)
    expect(getByText('alice@test.com')).toBeDefined()
  })

  it('shows role pill with translation key', () => {
    const { getByText } = render(<ProfileSummaryCard user={BASE_USER} />)
    expect(getByText('user.role.USER')).toBeDefined()
  })

  it('shows initials derived from username', () => {
    const { getByText } = render(<ProfileSummaryCard user={BASE_USER} />)
    expect(getByText('A')).toBeDefined()
  })

  it('shows initials for dotted username', () => {
    const user: User = { ...BASE_USER, username: 'john.doe' }
    const { getByText } = render(<ProfileSummaryCard user={user} />)
    expect(getByText('JD')).toBeDefined()
  })

  it('renders SUPER_ADMIN with a different gradient than USER', () => {
    const { container: adminContainer } = render(<ProfileSummaryCard user={{ ...BASE_USER, role: 'SUPER_ADMIN' }} />)
    const { container: userContainer } = render(<ProfileSummaryCard user={BASE_USER} />)
    const adminAvatar = adminContainer.querySelector('[style*="gradient"]') as HTMLElement
    const userAvatar = userContainer.querySelector('[style*="gradient"]') as HTMLElement
    expect(adminAvatar.style.background).not.toBe(userAvatar.style.background)
  })
})