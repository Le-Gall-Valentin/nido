import { fireEvent, render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Tooltip } from './Tooltip'

const tip = () => screen.queryByRole('tooltip')

describe('Tooltip, inside something already clickable', () => {
  function renderInRow() {
    const onRowClick = vi.fn()
    render(
      <button type="button" onClick={onRowClick}>
        Groceries
        <Tooltip label="Marie Dupont"><span data-testid="trigger">MD</span></Tooltip>
      </button>,
    )
    return { onRowClick, trigger: screen.getByTestId('trigger') }
  }

  it('shows the label while a mouse is over it', () => {
    const { trigger } = renderInRow()
    fireEvent.pointerEnter(trigger, { pointerType: 'mouse' })
    expect(tip()?.textContent).toBe('Marie Dupont')
    fireEvent.pointerLeave(trigger, { pointerType: 'mouse' })
    expect(tip()).toBeNull()
  })

  it('ignores the hover a phone makes up around a tap, and lets the tap reach the row', () => {
    const { trigger, onRowClick } = renderInRow()
    fireEvent.pointerEnter(trigger, { pointerType: 'touch' })
    fireEvent.click(trigger)
    expect(tip()).toBeNull()
    expect(onRowClick).toHaveBeenCalledOnce()
  })

  it('adds the label to what the row reads out', () => {
    renderInRow()
    expect(screen.getByRole('button', { name: /Groceries.*Marie Dupont/ })).toBeDefined()
  })
})

describe('Tooltip, standing alone', () => {
  function renderAlone() {
    render(<Tooltip label="Marie Dupont" standalone><span>MD</span></Tooltip>)
    return screen.getByRole('button', { name: 'Marie Dupont' })
  }

  function tap(element: Element) {
    fireEvent.pointerDown(element, { pointerType: 'touch' })
    fireEvent.focus(element)
    fireEvent.click(element)
  }

  it('is a button named by its label', () => {
    expect(renderAlone()).toBeDefined()
  })

  it('opens on a tap and closes on the next one', () => {
    const trigger = renderAlone()
    tap(trigger)
    expect(tip()?.textContent).toBe('Marie Dupont')
    tap(trigger)
    expect(tip()).toBeNull()
  })

  it('opens on a keyboard focus and closes when the focus leaves', () => {
    const trigger = renderAlone()
    fireEvent.focus(trigger)
    expect(tip()).not.toBeNull()
    fireEvent.blur(trigger)
    expect(tip()).toBeNull()
  })

  it('stays open when a mouse that hovered it clicks it', () => {
    const trigger = renderAlone()
    fireEvent.pointerEnter(trigger, { pointerType: 'mouse' })
    fireEvent.pointerDown(trigger, { pointerType: 'mouse' })
    fireEvent.focus(trigger)
    fireEvent.click(trigger)
    expect(tip()).not.toBeNull()
  })

  it('closes on a tap anywhere else', () => {
    const trigger = renderAlone()
    tap(trigger)
    fireEvent.pointerDown(document.body, { pointerType: 'touch' })
    expect(tip()).toBeNull()
  })

  it('closes when the page scrolls, since it would stay behind', () => {
    const trigger = renderAlone()
    tap(trigger)
    fireEvent.scroll(window)
    expect(tip()).toBeNull()
  })

  it('closes on Escape without letting the dialog around it close too', () => {
    const dialogEscape = vi.fn()
    document.addEventListener('keydown', dialogEscape)
    const trigger = renderAlone()
    fireEvent.focus(trigger)
    fireEvent.keyDown(trigger, { key: 'Escape' })
    expect(tip()).toBeNull()
    expect(dialogEscape).not.toHaveBeenCalled()
    fireEvent.keyDown(trigger, { key: 'Escape' })
    expect(dialogEscape).toHaveBeenCalledOnce()
    document.removeEventListener('keydown', dialogEscape)
  })

  it('draws the bubble at the root of the page, where no scrolling panel can cut it', () => {
    const trigger = renderAlone()
    tap(trigger)
    expect(tip()?.parentElement).toBe(document.body)
  })
})
