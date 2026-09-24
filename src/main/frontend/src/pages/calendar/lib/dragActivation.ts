import type { PointerActivationConstraint } from '@dnd-kit/core'

/**
 * When a press on a draggable chip becomes a drag.
 *
 * Never immediately. With no constraint, dnd-kit starts dragging on pointerdown and then swallows
 * the click that follows — so a chip that can be dragged can no longer be opened. On a mouse a few
 * pixels of movement is the signal, as on the tasks board; on touch it is a long press, because
 * here the whole chip is the handle and a swipe or a scroll crossing it must not start a drag.
 */
export function dragActivationConstraint(pointerIsFine: boolean): PointerActivationConstraint {
  return pointerIsFine ? { distance: 8 } : { delay: 250, tolerance: 5 }
}
