interface SwitchProps {
  checked: boolean
  /** Receives the state asked for: the opposite of `checked`. */
  onChange: (checked: boolean) => void
  disabled?: boolean
  title?: string
  'aria-label'?: string
  'aria-labelledby'?: string
  'aria-describedby'?: string
}

/** An on/off switch. Name it with `aria-label`, or with `aria-labelledby` when its label is on the page. */
export function Switch({ checked, onChange, disabled = false, title, ...aria }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      title={title}
      disabled={disabled}
      onClick={() => { if (!disabled) onChange(!checked) }}
      {...aria}
      className={`relative inline-flex h-[26px] w-[44px] shrink-0 items-center rounded-full border-0 transition-colors
        ${checked ? 'bg-accent' : 'bg-bg-4'}
        ${disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
    >
      <span
        aria-hidden="true"
        className={`pointer-events-none inline-block size-5 rounded-full bg-white shadow-[0_1px_2px_rgba(0,0,0,0.2)] transition-transform ${checked ? 'translate-x-[21px]' : 'translate-x-[3px]'}`}
      />
    </button>
  )
}
