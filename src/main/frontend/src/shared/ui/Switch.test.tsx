import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Switch } from './Switch'

describe('Switch', () => {
  it('is a switch that says whether it is on', () => {
    render(<Switch checked aria-label="Mail" onChange={vi.fn()} />)

    expect(screen.getByRole('switch', { name: 'Mail' }).getAttribute('aria-checked')).toBe('true')
  })

  it('asks for the opposite state when pressed', () => {
    const onChange = vi.fn()
    render(<Switch checked={false} aria-label="Mail" onChange={onChange} />)

    fireEvent.click(screen.getByRole('switch'))

    expect(onChange).toHaveBeenCalledWith(true)
  })

  it('does nothing while disabled', () => {
    const onChange = vi.fn()
    render(<Switch checked aria-label="Mail" disabled onChange={onChange} />)

    const control = screen.getByRole('switch') as HTMLButtonElement
    fireEvent.click(control)

    expect(control.disabled).toBe(true)
    expect(onChange).not.toHaveBeenCalled()
  })

  it('is named and described by the line it sits on', () => {
    render(
      <>
        <span id="label">Invitation</span>
        <span id="description">When someone invites you</span>
        <Switch checked aria-labelledby="label" aria-describedby="description" title="Why not" onChange={vi.fn()} />
      </>,
    )

    const control = screen.getByRole('switch', { name: 'Invitation' })
    expect(control.getAttribute('aria-describedby')).toBe('description')
    expect(control.getAttribute('title')).toBe('Why not')
    expect(control.getAttribute('type')).toBe('button')
  })
})
