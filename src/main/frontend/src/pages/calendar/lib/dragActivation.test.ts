import { describe, it, expect } from 'vitest'
import { dragActivationConstraint } from './dragActivation'

describe('dragActivationConstraint', () => {
  it('waits for real movement on a mouse, so a plain click stays a click', () => {
    // With no constraint the drag started on pointerdown and swallowed the click that followed:
    // every draggable chip on a desktop opened nothing.
    expect(dragActivationConstraint(true)).toEqual({ distance: 8 })
  })

  it('waits for a long press on touch, so scrolling and swiping keep the gesture', () => {
    expect(dragActivationConstraint(false)).toEqual({ delay: 250, tolerance: 5 })
  })
})
