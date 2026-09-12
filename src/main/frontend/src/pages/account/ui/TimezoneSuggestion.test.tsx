import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { TimezoneSuggestion } from './TimezoneSuggestion'
import type { SpaceSummary } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

const PERSONAL: SpaceSummary = {
  id: 'p-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤',
  myRole: 'OWNER', memberCount: 1, timezone: 'Europe/Paris',
}

function setup(overrides: {
  space?: SpaceSummary | undefined
  browser?: string
  onAccept?: Mock<(timezone: string) => Promise<void>>
} = {}) {
  const onAccept: Mock<(timezone: string) => Promise<void>> =
    overrides.onAccept ?? vi.fn<(timezone: string) => Promise<void>>().mockResolvedValue(undefined)
  const space = 'space' in overrides ? overrides.space : PERSONAL
  return {
    onAccept,
    ...render(<TimezoneSuggestion
      space={space} browserTimezone={overrides.browser ?? 'America/Toronto'} onAccept={onAccept} />),
  }
}

beforeEach(() => { localStorage.clear() })

describe('TimezoneSuggestion', () => {
  it('offers the move, naming both places', () => {
    setup()

    expect(screen.getByText(/timezone_suggestion\.body/)).toBeTruthy()
  })

  it('applies the browser zone when accepted', async () => {
    const { onAccept } = setup()

    fireEvent.click(screen.getByText('timezone_suggestion.accept'))

    await waitFor(() => expect(onAccept).toHaveBeenCalledWith('America/Toronto'))
  })

  it('goes away when turned down, and does not come back for the same place', () => {
    const { unmount } = setup()

    fireEvent.click(screen.getByText('timezone_suggestion.dismiss'))
    expect(screen.queryByText('timezone_suggestion.accept')).toBeNull()

    unmount()
    setup()
    expect(screen.queryByText('timezone_suggestion.accept')).toBeNull()
  })

  it('asks again once the traveller is somewhere new', () => {
    const { unmount } = setup()
    fireEvent.click(screen.getByText('timezone_suggestion.dismiss'))
    unmount()

    setup({ browser: 'Asia/Tokyo' })

    expect(screen.getByText('timezone_suggestion.accept')).toBeTruthy()
  })

  it('says nothing when the space already keeps the browser calendar', () => {
    setup({ browser: 'Europe/Paris' })

    expect(screen.queryByText('timezone_suggestion.accept')).toBeNull()
  })

  it('says nothing about a shared space', () => {
    setup({ space: { ...PERSONAL, type: 'SHARED' } })

    expect(screen.queryByText('timezone_suggestion.accept')).toBeNull()
  })
})
