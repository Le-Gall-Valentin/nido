import { useId, type TextareaHTMLAttributes } from 'react'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
}

export function Textarea({ label, className = '', ...props }: TextareaProps) {
  const generatedId = useId()
  const id = props.id ?? props.name ?? generatedId

  return (
    <div className="flex flex-1 flex-col gap-1.5">
      <label htmlFor={id} className="sr-only">
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
