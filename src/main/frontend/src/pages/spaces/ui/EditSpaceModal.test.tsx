import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { EditSpaceModal } from './EditSpaceModal'
import { NetworkError, ServerError } from '@/shared/lib'
import type { SpaceDetail } from '@/entities/space'
import type { UpdateSpaceInput } from '../model/ISpacesPageApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
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

const SPACE: SpaceDetail = {
  id: 's-1', type: 'SHARED', name: 'Chez nous', description: 'Notre appartement',
  accent: '#4a7fa0', glyph: '🌿', myRole: 'OWNER', memberCount: 2,
}

function setup(overrides: { space?: SpaceDetail; onUpdate?: Mock<(patch: UpdateSpaceInput) => Promise<void>> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onUpdate = overrides.onUpdate ?? vi.fn().mockResolvedValue(undefined)
  const space = overrides.space ?? SPACE
  const result = render(<EditSpaceModal space={space} onClose={onClose} onUpdate={onUpdate} onSuccess={onSuccess} />)
  return { ...result, onClose, onSuccess, onUpdate }
}

beforeEach(() => { vi.clearAllMocks() })

describe('EditSpaceModal — prefill', () => {
  it('prefills the name and description from the space', () => {
    const { getByLabelText } = setup()
    expect((getByLabelText('edit.name') as HTMLInputElement).value).toBe('Chez nous')
    expect((getByLabelText('edit.description') as HTMLInputElement).value).toBe('Notre appartement')
  })

  it('prefills an empty description when the space has none', () => {
    const { getByLabelText } = setup({ space: { ...SPACE, description: null } })
    expect((getByLabelText('edit.description') as HTMLInputElement).value).toBe('')
  })

  it('preselects the space\'s current accent and glyph', () => {
    const { getAllByLabelText } = setup()
    const accentOptions = getAllByLabelText('edit.accent_option', { exact: false })
    const glyphOptions = getAllByLabelText('edit.glyph_option', { exact: false })
    expect(accentOptions.find((o) => o.getAttribute('aria-pressed') === 'true')?.getAttribute('aria-label')).toContain('#4a7fa0')
    expect(glyphOptions.find((o) => o.getAttribute('aria-pressed') === 'true')?.getAttribute('aria-label')).toContain('🌿')
  })
})

describe('EditSpaceModal — dirty tracking', () => {
  it('submit is disabled when nothing changed', () => {
    const { getByText } = setup()
    expect((getByText('edit.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit is enabled once the name changes', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('edit.name'), { target: { value: 'Notre nid' } })
    expect((getByText('edit.submit') as HTMLButtonElement).disabled).toBe(false)
  })

  it('submit is disabled again once the field is reverted to its original value', () => {
    const { getByLabelText, getByText } = setup()
    const input = getByLabelText('edit.name')
    fireEvent.change(input, { target: { value: 'Notre nid' } })
    fireEvent.change(input, { target: { value: 'Chez nous' } })
    expect((getByText('edit.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit is disabled when the only change is invalid (name too long)', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('edit.name'), { target: { value: 'a'.repeat(81) } })
    expect((getByText('edit.submit') as HTMLButtonElement).disabled).toBe(true)
  })
})

describe('EditSpaceModal — partial submit', () => {
  it('sends only the changed name', async () => {
    const { getByLabelText, getByText, onUpdate, onSuccess } = setup()
    fireEvent.change(getByLabelText('edit.name'), { target: { value: 'Notre nid' } })
    fireEvent.click(getByText('edit.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith({ name: 'Notre nid' }))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('sends only the changed description, trimmed', async () => {
    const { getByLabelText, getByText, onUpdate } = setup()
    fireEvent.change(getByLabelText('edit.description'), { target: { value: '  Notre nouveau nid  ' } })
    fireEvent.click(getByText('edit.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith({ description: 'Notre nouveau nid' }))
  })

  it('sends an explicit empty string to clear the description', async () => {
    const { getByLabelText, getByText, onUpdate } = setup()
    fireEvent.change(getByLabelText('edit.description'), { target: { value: '' } })
    fireEvent.click(getByText('edit.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith({ description: '' }))
  })

  it('sends only the changed accent', async () => {
    const { getAllByLabelText, getByText, onUpdate } = setup()
    const options = getAllByLabelText('edit.accent_option', { exact: false })
    // SPACE.accent is SPACE_ACCENTS[1] ('#4a7fa0'); pick a genuinely different one.
    fireEvent.click(options[0])
    fireEvent.click(getByText('edit.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalled())
    expect(onUpdate.mock.calls[0][0]).not.toHaveProperty('name')
    expect(onUpdate.mock.calls[0][0]).not.toHaveProperty('description')
    expect(onUpdate.mock.calls[0][0]).toHaveProperty('accent')
  })

  it('sends only the changed glyph', async () => {
    const { getAllByLabelText, getByText, onUpdate } = setup()
    const options = getAllByLabelText('edit.glyph_option', { exact: false })
    fireEvent.click(options[2])
    fireEvent.click(getByText('edit.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalled())
    expect(onUpdate.mock.calls[0][0]).toEqual(expect.objectContaining({ glyph: expect.any(String) }))
    expect(onUpdate.mock.calls[0][0]).not.toHaveProperty('name')
  })
})

describe('EditSpaceModal — errors', () => {
  it('shows a server error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({ onUpdate: vi.fn().mockRejectedValue(new ServerError()) })
    fireEvent.change(getByLabelText('edit.name'), { target: { value: 'Notre nid' } })
    fireEvent.click(getByText('edit.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('edit.error.server')
  })

  it('shows a network error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({ onUpdate: vi.fn().mockRejectedValue(new NetworkError()) })
    fireEvent.change(getByLabelText('edit.name'), { target: { value: 'Notre nid' } })
    fireEvent.click(getByText('edit.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('edit.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('edit.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
