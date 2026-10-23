import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { BusyButton } from './BusyButton'

describe('BusyButton', () => {
  it('runs its action when nothing runs', () => {
    const onClick = vi.fn()
    render(<BusyButton type="button" busy={false} onClick={onClick}>Go</BusyButton>)
    const button = screen.getByRole('button', { name: 'Go' })
    fireEvent.click(button)
    expect(onClick).toHaveBeenCalledTimes(1)
    expect(button.getAttribute('aria-disabled')).toBeNull()
  })

  it('waits while an action runs, said so and dimmed, yet focusable and focused still', () => {
    const onClick = vi.fn()
    render(<BusyButton type="button" busy onClick={onClick}>Go</BusyButton>)
    const button = screen.getByRole('button', { name: 'Go' }) as HTMLButtonElement
    button.focus()
    fireEvent.click(button)
    expect(onClick).not.toHaveBeenCalled()
    expect(button.disabled).toBe(false)
    expect(document.activeElement).toBe(button)
    expect(button.getAttribute('aria-disabled')).toBe('true')
    expect(button.className).toContain('aria-disabled:opacity-50')
  })

  it('stays disabled when there is nothing to do, whatever runs', () => {
    render(<BusyButton type="button" busy={false} disabled onClick={vi.fn()}>Go</BusyButton>)
    expect((screen.getByRole('button', { name: 'Go' }) as HTMLButtonElement).disabled).toBe(true)
  })
})
