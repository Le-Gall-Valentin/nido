import { useId, type InputHTMLAttributes, type ReactNode } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  suffix?: ReactNode
  /** Keeps the label accessible to screen readers without showing it — for repeated rows (e.g. a list of ingredients) where a visible label per row is redundant. */
  srOnlyLabel?: boolean
}

export function Input({ label, suffix, srOnlyLabel, className = '', ...props }: InputProps) {
  const generatedId = useId()
  const id = props.id ?? props.name ?? generatedId

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className={srOnlyLabel ? 'sr-only' : 'text-[13px] font-semibold text-fg-1'}>
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          className={`w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:shadow-[0_0_0_3px_var(--color-accent-ring)] ${suffix ? 'pr-11' : ''} ${className}`}
          {...props}
        />
        {suffix && (
          <div className="absolute right-1.5 top-1/2 -translate-y-1/2">
            {suffix}
          </div>
        )}
      </div>
    </div>
  )
}