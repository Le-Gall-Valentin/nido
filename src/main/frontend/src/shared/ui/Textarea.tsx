import { useId, type TextareaHTMLAttributes } from 'react'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  /** Keeps the label accessible to screen readers without showing it — for repeated rows (e.g. a numbered list of steps) where a visible label per row is redundant. */
  srOnlyLabel?: boolean
}

export function Textarea({ label, srOnlyLabel, className = '', ...props }: TextareaProps) {
  const generatedId = useId()
  const id = props.id ?? props.name ?? generatedId

  return (
    <div className="flex flex-1 flex-col gap-1.5">
      <label htmlFor={id} className={srOnlyLabel ? 'sr-only' : 'text-[13px] font-semibold text-fg-1'}>
        {label}
      </label>
      <textarea
        id={id}
        className={`w-full resize-y rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] leading-[1.45] text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:shadow-[0_0_0_3px_var(--color-accent-ring)] ${className}`}
        {...props}
      />
    </div>
  )
}
