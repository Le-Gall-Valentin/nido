import { Loader2, type LucideIcon } from 'lucide-react'
import type { ComponentPropsWithRef, ReactNode } from 'react'
import { buttonClassName, type ButtonLook, type ButtonSize } from './buttonLook'

const ICON_SIZE: Record<ButtonSize, string> = { md: 'size-4', sm: 'size-3.5' }

interface ButtonProps extends ComponentPropsWithRef<'button'>, ButtonLook {
  isLoading?: boolean
  /** Drawn before the label, green on a secondary button so that it reads at a glance. */
  icon?: LucideIcon
  children: ReactNode
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon: Icon,
  isLoading = false,
  disabled,
  children,
  className = '',
  ...props
}: ButtonProps) {
  const iconClass = `${ICON_SIZE[size]} shrink-0`
  return (
    <button
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      data-variant={variant}
      className={buttonClassName({ variant, size, className })}
      {...props}
    >
      {isLoading ? (
        <>
          <Loader2 className={`${iconClass} animate-spin`} aria-hidden="true" />
          <span className="sr-only">{children}</span>
        </>
      ) : (
        <>
          {Icon && <Icon className={`${iconClass} ${variant === 'secondary' ? 'text-accent' : ''}`} aria-hidden="true" />}
          {children}
        </>
      )}
    </button>
  )
}
