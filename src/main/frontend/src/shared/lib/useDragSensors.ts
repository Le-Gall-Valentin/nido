import { KeyboardSensor, MouseSensor, TouchSensor, useSensor, useSensors, type KeyboardCoordinateGetter } from '@dnd-kit/core'

interface DragSensorOptions {
  /**
   * Also let the keyboard drag, moving by these coordinates — for a list whose drag starts from a
   * handle of its own. Left out where the drag starts from a card holding other buttons: the
   * keyboard sensor would take their Space and Enter.
   */
  keyboardCoordinates?: KeyboardCoordinateGetter
}

/**
 * How a drag starts, the same everywhere in the app. A mouse after 8px, so a click stays a click;
 * a finger after a 250ms press, so scrolling and swiping keep the gesture — TouchSensor blocks the
 * scroll only once the drag has started, which is why a draggable surface stays
 * `touch-manipulation`, never `touch-none`.
 */
export function useDragSensors({ keyboardCoordinates }: DragSensorOptions = {}) {
  const mouse = useSensor(MouseSensor, { activationConstraint: { distance: 8 } })
  const touch = useSensor(TouchSensor, { activationConstraint: { delay: 250, tolerance: 5 } })
  const keyboard = useSensor(KeyboardSensor, { coordinateGetter: keyboardCoordinates })
  return useSensors(mouse, touch, keyboardCoordinates ? keyboard : null)
}
