import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MethodCard } from './MethodCard'

describe('MethodCard', () => {
  it('is one button that names the method, says what it does and carries its tag', () => {
    const onSelect = vi.fn()
    render(<MethodCard method="APP" title="Authenticator app" description="The code your app shows." tag="Most secure" onSelect={onSelect} />)

    const card = screen.getByRole('button', { name: /Authenticator app/ })
    expect(card.textContent).toContain('The code your app shows.')
    expect(card.textContent).toContain('Most secure')
    fireEvent.click(card)
    expect(onSelect).toHaveBeenCalledOnce()
  })

  it('cannot be chosen while disabled', () => {
    const onSelect = vi.fn()
    render(<MethodCard method="MAIL" title="Code by email" description="Sent to j••••••e@x.fr" onSelect={onSelect} disabled />)

    fireEvent.click(screen.getByRole('button', { name: /Code by email/ }))
    expect(onSelect).not.toHaveBeenCalled()
  })
})
