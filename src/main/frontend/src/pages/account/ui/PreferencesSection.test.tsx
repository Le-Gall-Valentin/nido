import { render, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { PreferencesSection } from './PreferencesSection'
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const mockSetTheme = vi.fn()
const mockSetLanguage = vi.fn()

vi.mock('@/shared/lib', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/lib')>()
  return {
    ...actual,
    useTheme: () => ({ theme: 'system', setTheme: mockSetTheme }),
    useLanguage: () => ({ language: 'fr', setLanguage: mockSetLanguage }),
  }
})

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PreferencesSection', () => {
  it('renders theme and language labels', () => {
    const { getByText } = render(<PreferencesSection />)
    expect(getByText('preferences.theme_label')).toBeDefined()
    expect(getByText('preferences.language_label')).toBeDefined()
  })

  it('clicking Clair calls setTheme with light', () => {
    const { getByText } = render(<PreferencesSection />)
    fireEvent.click(getByText('preferences.theme_light'))
    expect(mockSetTheme).toHaveBeenCalledWith('light')
  })

  it('clicking Sombre calls setTheme with dark', () => {
    const { getByText } = render(<PreferencesSection />)
    fireEvent.click(getByText('preferences.theme_dark'))
    expect(mockSetTheme).toHaveBeenCalledWith('dark')
  })

  it('clicking Système calls setTheme with system', () => {
    const { getByText } = render(<PreferencesSection />)
    fireEvent.click(getByText('preferences.theme_system'))
    expect(mockSetTheme).toHaveBeenCalledWith('system')
  })

  it('clicking EN calls setLanguage with en', () => {
    const { getByText } = render(<PreferencesSection />)
    fireEvent.click(getByText('EN'))
    expect(mockSetLanguage).toHaveBeenCalledWith('en')
  })

  it('clicking FR calls setLanguage with fr', () => {
    const { getByText } = render(<PreferencesSection />)
    fireEvent.click(getByText('FR'))
    expect(mockSetLanguage).toHaveBeenCalledWith('fr')
  })
})