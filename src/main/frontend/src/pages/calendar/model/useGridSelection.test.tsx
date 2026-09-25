import { describe, it, expect, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { ScheduleChange } from '@/entities/calendar'
import { useGridSelection } from './useGridSelection'

// An hour column whose top sits at y = 0, so a pointer's y is the minute of the day it points at.
const column = document.createElement('div')
column.getBoundingClientRect = () => ({ top: 0 }) as DOMRect

function press(clientY: number, overrides: Partial<React.PointerEvent<HTMLElement>> = {}) {
  return {
    pointerType: 'mouse', button: 0, clientX: 0, clientY, target: column, currentTarget: column,
    preventDefault: vi.fn(), ...overrides,
  } as unknown as React.PointerEvent<HTMLElement>
}

const move = (clientY: number) => window.dispatchEvent(new MouseEvent('pointermove', { clientX: 0, clientY }))
const release = () => window.dispatchEvent(new Event('pointerup'))

function select(canAdd = true) {
  const onPick = vi.fn<(range: ScheduleChange) => void>()
  const hook = renderHook(() => useGridSelection(canAdd ? onPick : undefined))
  return { ...hook, onPick }
}

describe('useGridSelection', () => {
  it('picks out the time pressed, dragged over and released', () => {
    const { result, onPick } = select()
    act(() => result.current.startHours(press(9 * 60), '2026-10-06'))
    // Down to the quarter of an hour the pointer is in, 10:30, which the selection takes whole.
    act(() => { move(10 * 60 + 30) })
    expect(result.current.shown?.range).toMatchObject({ startDate: '2026-10-06', startTime: '09:00', endTime: '10:45' })
    act(() => { release() })
    expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ startTime: '09:00', endTime: '10:45' }))
    expect(result.current.shown).toBeNull()
  })

  it('lets Escape drop the selection', () => {
    const { result, onPick } = select()
    act(() => result.current.startHours(press(9 * 60), '2026-10-06'))
    act(() => { window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })) })
    act(() => { release() })
    expect(result.current.shown).toBeNull()
    expect(onPick).not.toHaveBeenCalled()
  })

  it('drops the selection when the window loses focus, rather than keep it hanging on the pointer', () => {
    // Alt-Tab mid-selection: the release happens elsewhere and never comes back. The next click
    // here would otherwise create an event over a time nobody chose.
    const { result, onPick } = select()
    act(() => result.current.startHours(press(9 * 60), '2026-10-06'))
    act(() => { window.dispatchEvent(new Event('blur')) })
    expect(result.current.shown).toBeNull()
    act(() => { release() })
    expect(onPick).not.toHaveBeenCalled()
  })

  it('drops the selection when the browser takes the pointer back', () => {
    const { result, onPick } = select()
    act(() => result.current.startHours(press(9 * 60), '2026-10-06'))
    act(() => { window.dispatchEvent(new Event('pointercancel')) })
    expect(result.current.shown).toBeNull()
    act(() => { release() })
    expect(onPick).not.toHaveBeenCalled()
  })

  it('leaves a finger, a right click and a press on an event to their own gestures', () => {
    const { result } = select()
    const event = document.createElement('button')
    act(() => result.current.startHours(press(9 * 60, { pointerType: 'touch' }), '2026-10-06'))
    act(() => result.current.startHours(press(9 * 60, { button: 2 }), '2026-10-06'))
    act(() => result.current.startHours(press(9 * 60, { target: event }), '2026-10-06'))
    expect(result.current.shown).toBeNull()
  })

  it('does nothing for someone who cannot add an event', () => {
    const { result } = select(false)
    act(() => result.current.startHours(press(9 * 60), '2026-10-06'))
    expect(result.current.shown).toBeNull()
  })
})
