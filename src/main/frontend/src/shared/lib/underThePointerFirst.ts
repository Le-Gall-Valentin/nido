import { pointerWithin, rectIntersection, type CollisionDetection } from '@dnd-kit/core'

/**
 * Where a dragged item lands: the drop zone under the pointer, when there is one — on a phone, where
 * zones are stacked, that is where the finger is, not whichever the dragged copy overlaps most. Past
 * a zone's end, which is short while it holds little, the one the copy overlaps still takes it.
 */
export const underThePointerFirst: CollisionDetection = (args) => {
  const underPointer = pointerWithin(args)
  return underPointer.length > 0 ? underPointer : rectIntersection(args)
}
