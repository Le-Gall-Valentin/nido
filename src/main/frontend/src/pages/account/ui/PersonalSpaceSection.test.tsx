import { useState } from 'react'
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

  it('follows the space when it changes elsewhere, instead of holding a stale draft', async () => {
    // Found by clicking through the real application, not by a test. The suggestion banner above this
    // section can move the calendar on its own; when it did, this form kept showing the zone the user
    // had picked here a moment earlier and left its save button enabled — so the next click would have
    // silently undone the change that had just been accepted.
    const { getByLabelText, getByText, rerender } = setup()
    fireEvent.change(getByLabelText('personal_space.timezone'), { target: { value: 'Europe/Lisbon' } })
    expect((getByLabelText('personal_space.timezone') as HTMLSelectElement).value).toBe('Europe/Lisbon')

    rerender(<PersonalSpaceSection space={{ ...PERSONAL, timezone: 'America/Toronto' }} onSave={vi.fn()} />)

    expect((getByLabelText('personal_space.timezone') as HTMLSelectElement).value).toBe('America/Toronto')
    expect((getByText('personal_space.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('does not resurrect an old draft when the space comes back to where it started', () => {
    // Driven through a parent that owns the space, because that is how it happens: the banner and this
    // section are siblings under one page, and rerender() with a different prop remounts the component
    // — which resets its state and hides the very bug being tested. The browser found this one; the
    // first two attempts at a fix passed a rerender-based test and were still broken on screen.
    const TORONTO = { ...PERSONAL, timezone: 'America/Toronto' }
    function Host() {
      const [space, setSpace] = useState<SpaceSummary>(PERSONAL)
      return (
        <>
          <button type="button" onClick={() => setSpace(TORONTO)}>banner-accepts-toronto</button>
          <PersonalSpaceSection space={space} onSave={vi.fn()} />
        </>
      )
    }
    render(<Host />)
    // Re-queried after each step rather than captured once: the element is replaced on re-render, and
    // a stale handle reports the value it had before, which is how a broken fix can look like a pass.
    const zone = () => screen.getByLabelText('personal_space.timezone') as HTMLSelectElement
    fireEvent.change(zone(), { target: { value: 'Europe/Lisbon' } })
    expect(zone().value).toBe('Europe/Lisbon')

    fireEvent.click(screen.getByText('banner-accepts-toronto'))

    // The space is Toronto now, so that is what the form must show — the Lisbon draft is spent.
    expect(zone().value).toBe('America/Toronto')
    expect((screen.getByText('personal_space.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('keeps what the user is choosing while the space does not move', async () => {
    // The other half: re-rendering for any other reason must not throw away a pick in progress.
    const { getByLabelText, rerender } = setup()
    fireEvent.change(getByLabelText('personal_space.timezone'), { target: { value: 'Europe/Lisbon' } })

    rerender(<PersonalSpaceSection space={PERSONAL} onSave={vi.fn()} />)

    expect((getByLabelText('personal_space.timezone') as HTMLSelectElement).value).toBe('Europe/Lisbon')
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
