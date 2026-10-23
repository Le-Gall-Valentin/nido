import { twMerge } from 'tailwind-merge'

export type ButtonVariant = 'secondary' | 'primary' | 'danger'
export type ButtonSize = 'md' | 'sm'

const SIZE: Record<ButtonSize, string> = {
  md: 'flex gap-[7px] rounded-[10px] px-[18px] py-[11px] text-sm',
  // A small button sits in a row or a section title: its label stays on one line.
  sm: 'inline-flex gap-1.5 whitespace-nowrap rounded-lg px-2.5 py-1 text-[12.5px]',
}

// The primary button's own colours, the same in both themes: the dark theme's accent is too light under white text.
const VARIANT: Record<ButtonVariant, string> = {
  secondary: 'border-border bg-bg-1 text-fg-2 hover:bg-bg-2 hover:text-fg-0',
  primary: 'border-transparent bg-[var(--btn-primary-bg)] text-[var(--btn-primary-fg)] hover:brightness-110',
  // What cannot be undone: deleting, leaving, turning a protection off.
  danger: 'border-transparent bg-status-red text-bg-0 hover:brightness-90',
}

// Lifted off the page at full size; a small one stays flat among the rows it sits in.
const PRIMARY_SHADOW = 'shadow-[var(--btn-primary-shadow)]'

export interface ButtonLook {
  variant?: ButtonVariant
  size?: ButtonSize
  className?: string
}

/** The button's classes, for what must look like one without being one — a link. The caller's classes win. */
export function buttonClassName({ variant = 'secondary', size = 'md', className = '' }: ButtonLook = {}) {
  return twMerge(
    'items-center justify-center border-[1.5px] font-semibold transition-colors disabled:opacity-50',
    SIZE[size],
    VARIANT[variant],
    variant === 'primary' && size === 'md' ? PRIMARY_SHADOW : '',
    className,
  )
}
