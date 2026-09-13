import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, type Mock } from 'vitest'
import { PersonalSpaceSection } from './PersonalSpaceSection'
import type { SpaceSummary } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const PERSONAL: SpaceSummary = {
  id: 'p-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤',
  myRole: 'OWNER', memberCount: 1, timezone: 'Europe/Paris',
}

function setup(overrides: { space?: SpaceSummary | undefined
                            onSave?: Mock<(timezone: string) => Promise<void>> } = {}) {
  const onSave: Mock<(timezone: string) => Promise<void>> =
    overrides.onSave ?? vi.fn<(timezone: string) => Promise<void>>().mockResolvedValue(undefined)
  const space = 'space' in overrides ? overrides.space : PERSONAL
  return { onSave, ...render(<PersonalSpaceSection space={space} onSave={onSave} />) }
}

describe('PersonalSpaceSection', () => {
  it('opens on the calendar the personal space keeps', () => {
    // The whole reason this section exists: the personal space has no detail page to edit, because
    // there is nothing else in it to change — but its calendar has to be reachable somewhere.
    const { getByLabelText } = setup()

    expect((getByLabelText('personal_space.timezone') as HTMLSelectElement).value).toBe('Europe/Paris')
  })

  it('says so when the save fails, instead of just stopping', async () => {
    // The button releases either way, so without this the screen is indistinguishable from a save
    // that worked: same form, same values, and the calendar silently unchanged on the server.
    const onSave = vi.fn<(timezone: string) => Promise<void>>().mockRejectedValue(new Error('network'))
    const { getByLabelText, getByText } = setup({ onSave })

    fireEvent.change(getByLabelText('personal_space.timezone'), { target: { value: 'America/Toronto' } })
    fireEvent.click(getByText('personal_space.submit'))

    expect(await screen.findByText('personal_space.error')).toBeDefined()
  })

  it('drops the failure message once a retry works', async () => {
    const onSave = vi.fn<(timezone: string) => Promise<void>>()
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValue(undefined)
    const { getByLabelText, getByText } = setup({ onSave })
    fireEvent.change(getByLabelText('personal_space.timezone'), { target: { value: 'America/Toronto' } })
    fireEvent.click(getByText('personal_space.submit'))
    await screen.findByText('personal_space.error')

    fireEvent.click(getByText('personal_space.submit'))

    await waitFor(() => expect(screen.queryByText('personal_space.error')).toBeNull())
  })

  it('saves the chosen calendar', async () => {
    const { getByLabelText, getByText, onSave } = setup()

    fireEvent.change(getByLabelText('personal_space.timezone'), { target: { value: 'America/Toronto' } })
    fireEvent.click(getByText('personal_space.submit'))

    await waitFor(() => expect(onSave).toHaveBeenCalledWith('America/Toronto'))
  })

  it('will not save what is already saved', () => {
    const { getByText } = setup()

    expect((getByText('personal_space.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('says nothing at all while the space is still loading', () => {
    // Rendering an empty select would offer to move a calendar whose current value is unknown.
    setup({ space: undefined })

    expect(screen.queryByLabelText('personal_space.timezone')).toBeNull()
  })
})
