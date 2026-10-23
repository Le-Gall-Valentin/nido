/**
 * One filter of the calendar: a real button with aria-pressed rather than a styled div, so its on/off state is
 * announced rather than only coloured. Off, it is greyed with fg-3, the calendar's own faded colour (fg-4 is defined
 * nowhere: see the month view).
 */
export function FilterChip({ on, dotClass, label, onToggle }: {
  on: boolean
  dotClass: string
  label: string
  onToggle: () => void
}) {
  return (
    <button
      type="button"
      aria-pressed={on}
      onClick={onToggle}
      className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold
        ${on ? 'border-border-2 bg-bg-1 text-fg-1' : 'border-border bg-transparent text-fg-3'}`}
    >
      <span aria-hidden className={`size-1.5 rounded-full ${on ? dotClass : 'bg-fg-3'}`} />
      {label}
    </button>
  )
}
