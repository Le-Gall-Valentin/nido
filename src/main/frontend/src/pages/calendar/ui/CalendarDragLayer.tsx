import { useEffect, useRef, useState, type ReactNode } from 'react'
import {
  DndContext, DragOverlay, MouseSensor, TouchSensor, pointerWithin, useSensor, useSensors,
  type DragEndEvent, type DragMoveEvent, type DragStartEvent,
} from '@dnd-kit/core'
import type { CalendarOccurrence } from '@/entities/calendar'
import type { CalendarView } from '../lib/calendarWindow'
import type { DragData, DragIntent, DropData, ScheduleChange } from '../lib/dragTypes'
import { resolveDrop } from '../lib/dragResolution'
import { horizontalZone, verticalZone, type Zone } from '../lib/edgeDwell'
import { HOUR_HEIGHT, minutesAt, snap } from '../lib/timeMath'
import { useEdgeNavigation } from '../model/useEdgeNavigation'
import { useWideLayout } from '../model/useWideLayout'
import { DragGhost } from './DragGhost'
import { EdgeCue } from './EdgeCue'

interface Props {
  enabled: boolean
  view: CalendarView
  onShift: (direction: -1 | 1) => void
  onDragStart: () => void
  onApply: (occurrence: CalendarOccurrence, change: ScheduleChange) => void
  children: ReactNode
}

interface Point { x: number; y: number }

/** Where the drag started. A mouse activates on a MouseEvent, a finger on a TouchEvent. */
function startPoint(event: Event): Point {
  if ('touches' in event) {
    const touch = (event as TouchEvent).touches[0] ?? (event as TouchEvent).changedTouches[0]
    return touch ? { x: touch.clientX, y: touch.clientY } : { x: 0, y: 0 }
  }
  const mouse = event as MouseEvent
  return { x: mouse.clientX, y: mouse.clientY }
}

/** The nearest ancestor that actually scrolls — on this app it is <main>, not the window. */
function scrollParentOf(element: HTMLElement | null): HTMLElement | null {
  let node = element?.parentElement ?? null
  while (node) {
    const { overflowY } = getComputedStyle(node)
    if ((overflowY === 'auto' || overflowY === 'scroll') && node.scrollHeight > node.clientHeight) return node
    node = node.parentElement
  }
  return null
}

/**
 * The one drag context for every view. It measures (pointer, column, edge); resolveDrop decides;
 * onApply writes. The ghost is a DragOverlay, independent of the grid, which is what lets a drag
 * survive the period changing underneath it.
 *
 * The dragged item is kept in this layer's state from the start: once a period change unmounts the
 * source, dnd-kit's `active.data` is an empty object, and a drop read from it would save nothing.
 */
export function CalendarDragLayer({ enabled, view, onShift, onDragStart, onApply, children }: Props) {
  const wide = useWideLayout()
  // A mouse after 8px, so a click stays a click; a finger after a 250ms press, so scrolling and
  // swiping keep the gesture — and TouchSensor blocks the scroll only once the drag has started.
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 250, tolerance: 5 } }),
  )
  const container = useRef<HTMLDivElement>(null)
  const [intent, setIntent] = useState<DragIntent | null>(null)
  const [preview, setPreview] = useState<ScheduleChange | null>(null)
  const grabOffset = useRef(0)
  const vertical = intent?.kind === 'move' && intent.from === 'row'

  // The pointer itself, read from the input events. dnd-kit's `delta` adds whatever its scrollers
  // scrolled, which is right for moving a node and wrong for asking what lies under the pointer:
  // auto-scrolling the week grid would shift every drop by the distance scrolled.
  const pointer = useRef<Point>({ x: 0, y: 0 })
  const stopTracking = useRef<(() => void) | null>(null)
  const trackPointer = (from: Point) => {
    pointer.current = from
    const onMouse = (event: MouseEvent) => { pointer.current = { x: event.clientX, y: event.clientY } }
    const onTouch = (event: TouchEvent) => {
      const touch = event.touches[0]
      if (touch) pointer.current = { x: touch.clientX, y: touch.clientY }
    }
    window.addEventListener('mousemove', onMouse, { capture: true, passive: true })
    window.addEventListener('touchmove', onTouch, { capture: true, passive: true })
    stopTracking.current = () => {
      window.removeEventListener('mousemove', onMouse, { capture: true })
      window.removeEventListener('touchmove', onTouch, { capture: true })
      stopTracking.current = null
    }
  }
  useEffect(() => () => stopTracking.current?.(), [])

  const shift = (direction: -1 | 1) => {
    onShift(direction)
    if (vertical) {
      // The phone list starts over from Monday going forward, from Sunday going back.
      const scroller = scrollParentOf(container.current)
      requestAnimationFrame(() => scroller?.scrollTo({ top: direction > 0 ? 0 : scroller.scrollHeight }))
    }
  }
  const edges = useEdgeNavigation(shift)

  const minutesFor = (event: DragMoveEvent | DragEndEvent, current: DragIntent): number | null => {
    const data = event.over?.data.current as DropData | undefined
    const column = data?.column?.()
    if (!column) return null
    const minutes = minutesAt(pointer.current.y, column.getBoundingClientRect().top)
    return current.kind === 'move' ? Math.max(0, minutes - grabOffset.current) : minutes
  }

  const zoneFor = (current: DragIntent): Zone => {
    const { x, y } = pointer.current
    if (current.kind === 'move' && current.from === 'row') {
      const scroller = scrollParentOf(container.current)
      if (!scroller) return 0
      const rect = scroller.getBoundingClientRect()
      return verticalZone(y, rect, scroller.scrollTop > 0,
        scroller.scrollTop + scroller.clientHeight < scroller.scrollHeight - 1)
    }
    // The phone day view only changes time; a finger near the side of a narrow screen must not flip days.
    if (!wide || !container.current) return 0
    return horizontalZone(x, container.current.getBoundingClientRect())
  }

  const handleStart = (event: DragStartEvent) => {
    const data = event.active.data.current as DragData | undefined
    if (!data) return
    onDragStart()
    setIntent(data.intent)
    setPreview(null)
    const start = startPoint(event.activatorEvent)
    trackPointer(start)
    // Grabbing a two-hour block by its middle must not make it jump so its top meets the pointer.
    // Measured on the grabbed element: dnd-kit has not measured its own rect yet at this point.
    const grabbed = (event.activatorEvent.target as Element | null)?.closest('[data-draggable]')
    const top = grabbed?.getBoundingClientRect().top
    grabOffset.current = data.intent.kind === 'move' && data.intent.from === 'grid' && top !== undefined
      ? snap(((start.y - top) / HOUR_HEIGHT) * 60) : 0
  }

  const handleMove = (event: DragMoveEvent) => {
    if (!intent) return
    edges.update(zoneFor(intent))
    const data = event.over?.data.current as DropData | undefined
    setPreview(data ? resolveDrop(intent, data.target, minutesFor(event, intent)) : null)
  }

  const finish = () => { edges.stop(); stopTracking.current?.(); setIntent(null); setPreview(null) }

  const handleEnd = (event: DragEndEvent) => {
    const current = intent
    const data = event.over?.data.current as DropData | undefined
    const minutes = current ? minutesFor(event, current) : null
    finish()
    if (!current || !data) return
    const change = resolveDrop(current, data.target, minutes)
    if (change) onApply(current.occurrence, change)
  }

  if (!enabled) return <div ref={container}>{children}</div>

  return (
    <DndContext sensors={sensors} collisionDetection={pointerWithin}
      onDragStart={handleStart} onDragMove={handleMove} onDragEnd={handleEnd} onDragCancel={finish}>
      <div ref={container} className="relative">
        {children}
        <EdgeCue armed={edges.armed} view={view} vertical={vertical} />
      </div>
      <DragOverlay dropAnimation={null}>
        {intent && <DragGhost intent={intent} preview={preview} />}
      </DragOverlay>
    </DndContext>
  )
}
