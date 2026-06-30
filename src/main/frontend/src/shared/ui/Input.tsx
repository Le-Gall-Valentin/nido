import { useId, type InputHTMLAttributes, type ReactNode } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  suffix?: ReactNode
}

export function Input({ label, suffix, className = '', ...props }: InputProps) {
  const generatedId = useId()
  const id = props.id ?? props.name ?? generatedId

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-xs font-medium text-fg-1">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          className={`w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] ${suffix ? 'pr-11' : ''} ${className}`}
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