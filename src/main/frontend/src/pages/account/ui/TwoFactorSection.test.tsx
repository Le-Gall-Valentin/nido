import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { TwoFactorSection } from './TwoFactorSection'
import type { User } from '@/entities/user'
import type { ITotpEnrollApi } from '@/features/totp'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', () => ({
  Button: ({ children, onClick, disabled, style, className }: React.ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button onClick={onClick} disabled={disabled} style={style} className={className}>{children}</button>
  ),
  Dialog: ({ open, children, onClose }: { open: boolean; children: React.ReactNode; onClose: () => void; title: string; maxWidth?: string }) =>
    open ? <div data-testid="enable-dialog"><button onClick={onClose}>close-dialog</button>{children}</div> : null,
  CTA_BUTTON_STYLE: {},
}))

vi.mock('@/features/totp', () => ({
  TotpSetupFlow: ({ onSuccess, onDismiss }: { onSuccess: () => void; onDismiss: () => void }) => (
    <div>
      <button onClick={onSuccess}>totp-success</button>
      <button onClick={onDismiss}>totp-dismiss</button>
    </div>
  ),
}))

vi.mock('./DisableTotpModal', () => ({
  DisableTotpModal: ({ open, onSuccess, onClose }: { open: boolean; onSuccess: () => void; onClose: () => void; onDisable: (code: string) => Promise<void> }) =>
    open ? (
      <div data-testid="disable-modal">
        <button onClick={onSuccess}>disable-success</button>
        <button onClick={onClose}>close-modal</button>
      </div>
    ) : null,
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}
const ENABLED_USER: User = { ...BASE_USER, totpEnabled: true }

const mockEnrollApi: ITotpEnrollApi = {
  setup: vi.fn(),
  confirm: vi.fn(),
  getStatus: vi.fn(),
  disable: vi.fn(),
}

describe('TwoFactorSection', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows disabled status when totpEnabled is false', () => {
    const { getByText } = render(
      <TwoFactorSection user={BASE_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    expect(getByText('twofa.status_disabled')).toBeDefined()
  })

  it('shows enabled status when totpEnabled is true', () => {
    const { getByText } = render(
      <TwoFactorSection user={ENABLED_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    expect(getByText('twofa.status_enabled')).toBeDefined()
  })

  it('shows enable button when 2FA is disabled', () => {
    const { getByText } = render(
      <TwoFactorSection user={BASE_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    expect(getByText('twofa.btn_enable')).toBeDefined()
  })

  it('shows disable button when 2FA is enabled', () => {
    const { getByText } = render(
      <TwoFactorSection user={ENABLED_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    expect(getByText('twofa.btn_disable')).toBeDefined()
  })

  it('opens setup dialog when enable button is clicked', () => {
    const { getByText, getByTestId } = render(
      <TwoFactorSection user={BASE_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_enable'))
    expect(getByTestId('enable-dialog')).toBeDefined()
  })

  it('opens disable modal when disable button is clicked', () => {
    const { getByText, getByTestId } = render(
      <TwoFactorSection user={ENABLED_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_disable'))
    expect(getByTestId('disable-modal')).toBeDefined()
  })

  it('calls onPatch with totpEnabled true and shows success flash after setup', () => {
    const onPatch = vi.fn()
    const { getByText } = render(
      <TwoFactorSection user={BASE_USER} onPatch={onPatch} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_enable'))
    fireEvent.click(getByText('totp-success'))
    expect(onPatch).toHaveBeenCalledWith({ totpEnabled: true })
    expect(getByText('twofa.success_enabled')).toBeDefined()
  })

  it('calls onPatch with totpEnabled false and shows success flash after disable', () => {
    const onPatch = vi.fn()
    const { getByText } = render(
      <TwoFactorSection user={ENABLED_USER} onPatch={onPatch} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_disable'))
    fireEvent.click(getByText('disable-success'))
    expect(onPatch).toHaveBeenCalledWith({ totpEnabled: false })
    expect(getByText('twofa.success_disabled')).toBeDefined()
  })

  it('closes dialog when dismiss is clicked', () => {
    const { getByText, queryByTestId } = render(
      <TwoFactorSection user={BASE_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_enable'))
    fireEvent.click(getByText('totp-dismiss'))
    expect(queryByTestId('enable-dialog')).toBeNull()
  })

  it('clears flashTimer on unmount', () => {
    const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout')
    const { getByText, unmount } = render(
      <TwoFactorSection user={BASE_USER} onPatch={vi.fn()} enrollApi={mockEnrollApi} />
    )
    fireEvent.click(getByText('twofa.btn_enable'))
    fireEvent.click(getByText('totp-success'))
    unmount()
    expect(clearTimeoutSpy).toHaveBeenCalled()
    clearTimeoutSpy.mockRestore()
  })
})