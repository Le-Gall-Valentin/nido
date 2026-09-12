import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { CreateSpaceModal } from './CreateSpaceModal'
import { browserTimezone, NetworkError, ServerError } from '@/shared/lib'
import type { SpaceDetail } from '@/entities/space'
import type { CreateSpaceInput } from '../model/ISpacesPageApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  CTA_BUTTON_STYLE: {},
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
  Input: ({ label, name, value, onChange, disabled, placeholder, autoFocus }: {
    label: string; name: string; value: string;
    onChange: React.ChangeEventHandler<HTMLInputElement>; disabled?: boolean; placeholder?: string; autoFocus?: boolean
  }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} value={value} onChange={onChange} disabled={disabled} placeholder={placeholder} autoFocus={autoFocus} />
    </div>
  ),
}))

const CREATED: SpaceDetail = {
  id: 's-1', type: 'SHARED', name: 'Chez nous', description: null,
  accent: '#4a7fa0', glyph: '🌿', myRole: 'OWNER', memberCount: 1, timezone: 'Europe/Paris',
}

function setup(overrides: { onCreate?: Mock<(input: CreateSpaceInput) => Promise<SpaceDetail>> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onCreate = overrides.onCreate ?? vi.fn().mockResolvedValue(CREATED)
  const result = render(<CreateSpaceModal onClose={onClose} onCreate={onCreate} onSuccess={onSuccess} />)
  return { ...result, onClose, onSuccess, onCreate }
}

beforeEach(() => { vi.clearAllMocks() })

describe('CreateSpaceModal — validation', () => {
  it('submit is disabled with an empty name', () => {
    const { getByText } = setup()
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit is enabled once a name is entered', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(false)
  })

  it('defaults to the first accent and glyph of the palette', () => {
    const { getAllByLabelText } = setup()
    expect(getAllByLabelText('create.accent_option', { exact: false })[0].getAttribute('aria-pressed')).toBe('true')
  })
})

describe('CreateSpaceModal — submit', () => {
  it('calls onCreate with the trimmed name, description, accent and glyph', async () => {
    const { getByLabelText, getByText, onCreate, onSuccess } = setup()
    fireEvent.change(getByLabelText('create.name'), { target: { value: '  Chez nous  ' } })
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith(expect.objectContaining({ name: 'Chez nous' })))
    expect(onSuccess).toHaveBeenCalledWith(CREATED)
  })

  it('omits an empty description', async () => {
    const { getByLabelText, getByText, onCreate } = setup()
    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalled())
    expect(onCreate.mock.calls[0][0].description).toBeUndefined()
  })

  it('shows a server error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({ onCreate: vi.fn().mockRejectedValue(new ServerError()) })
    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.server')
  })

  it('shows a network error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({ onCreate: vi.fn().mockRejectedValue(new NetworkError()) })
    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('create.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})

describe('CreateSpaceModal — accent and glyph pickers', () => {
  it('selects a different accent on click', () => {
    const { getAllByLabelText } = setup()
    const options = getAllByLabelText('create.accent_option', { exact: false })
    fireEvent.click(options[1])
    expect(options[1].getAttribute('aria-pressed')).toBe('true')
    expect(options[0].getAttribute('aria-pressed')).toBe('false')
  })

  it('selects a different glyph on click', () => {
    const { getAllByLabelText } = setup()
    const options = getAllByLabelText('create.glyph_option', { exact: false })
    fireEvent.click(options[2])
    expect(options[2].getAttribute('aria-pressed')).toBe('true')
  })
})

describe('CreateSpaceModal — timezone', () => {
  it('opens on the calendar the creator is actually in', () => {
    // Nobody should have to configure this in the ordinary case: create a household from where you
    // live and it starts on your calendar, not on the server's.
    const { getByLabelText } = setup()

    expect((getByLabelText('create.timezone') as HTMLSelectElement).value).toBe(browserTimezone())
  })

  it('sends the chosen zone, which need not be the detected one', async () => {
    // Somebody setting up the household they are moving into next month, from somewhere else.
    const { getByLabelText, getByText, onCreate } = setup()

    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    fireEvent.change(getByLabelText('create.timezone'), { target: { value: 'Europe/Paris' } })
    fireEvent.click(getByText('create.submit'))

    await waitFor(() => expect(onCreate).toHaveBeenCalledWith(
      expect.objectContaining({ timezone: 'Europe/Paris' })))
  })

  it('sends the detected zone when the creator leaves it alone', async () => {
    const { getByLabelText, getByText, onCreate } = setup()

    fireEvent.change(getByLabelText('create.name'), { target: { value: 'Chez nous' } })
    fireEvent.click(getByText('create.submit'))

    await waitFor(() => expect(onCreate).toHaveBeenCalledWith(
      expect.objectContaining({ timezone: browserTimezone() })))
  })
})
