import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { useFocusTrap } from './useFocusTrap'

function makeContainer(...tagNames: string[]): HTMLDivElement {
  const container = document.createElement('div')
  for (const tag of tagNames) {
    const el = document.createElement(tag)
    if (tag === 'button' || tag === 'input') {
      // ensure they are focusable
    }
    container.appendChild(el)
  }
  document.body.appendChild(container)
  return container
}

beforeEach(() => {
  document.body.innerHTML = ''
})

describe('useFocusTrap', () => {
  it('does nothing when disabled', () => {
    const container = makeContainer('button', 'button')
    const ref = { current: container }
    const { unmount } = renderHook(() => useFocusTrap(ref, false))
    expect(document.activeElement).toBe(document.body)
    unmount()
  })

  it('focuses first focusable element when enabled', () => {
    const container = makeContainer('button', 'button')
    const ref = { current: container }
    const buttons = container.querySelectorAll('button')
    renderHook(() => useFocusTrap(ref, true))
    expect(document.activeElement).toBe(buttons[0])
  })

  it('does not re-focus when focus is already inside the container', () => {
    const container = makeContainer('button', 'button')
    const buttons = Array.from(container.querySelectorAll('button'))
    buttons[1].focus()
    const ref = { current: container }
    renderHook(() => useFocusTrap(ref, true))
    expect(document.activeElement).toBe(buttons[1])
  })

  it('Tab on last element wraps focus to first', () => {
    const container = makeContainer('button', 'button')
    const ref = { current: container }
    const buttons = Array.from(container.querySelectorAll<HTMLButtonElement>('button'))
    renderHook(() => useFocusTrap(ref, true))
    buttons[buttons.length - 1].focus()
    act(() => {
      container.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }))
    })
    expect(document.activeElement).toBe(buttons[0])
  })

  it('Shift+Tab on first element wraps focus to last', () => {
    const container = makeContainer('button', 'button')
    const ref = { current: container }
    const buttons = Array.from(container.querySelectorAll<HTMLButtonElement>('button'))
    renderHook(() => useFocusTrap(ref, true))
    buttons[0].focus()
    act(() => {
      container.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true }))
    })
    expect(document.activeElement).toBe(buttons[buttons.length - 1])
  })

  it('Tab on non-last element does not redirect focus', () => {
    const container = makeContainer('button', 'button', 'button')
    const ref = { current: container }
    const buttons = Array.from(container.querySelectorAll<HTMLButtonElement>('button'))
    renderHook(() => useFocusTrap(ref, true))
    buttons[0].focus()
    act(() => {
      container.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }))
    })
    expect(document.activeElement).toBe(buttons[0])
  })

  it('does nothing on Tab when there are no focusable elements', () => {
    const container = document.createElement('div')
    document.body.appendChild(container)
    const ref = { current: container }
    renderHook(() => useFocusTrap(ref, true))
    act(() => {
      container.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }))
    })
    expect(document.activeElement).toBe(document.body)
  })

  it('removes event listener on cleanup', () => {
    const container = makeContainer('button', 'button')
    const ref = { current: container }
    const buttons = Array.from(container.querySelectorAll<HTMLButtonElement>('button'))
    const { unmount } = renderHook(() => useFocusTrap(ref, true))
    unmount()
    buttons[buttons.length - 1].focus()
    act(() => {
      container.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }))
    })
    expect(document.activeElement).toBe(buttons[buttons.length - 1])
  })
})