import { AlertTriangle, CheckCircle2 } from 'lucide-react'

const VARIANT_CLASS = {
  error: 'border-status-red/25 bg-status-red-dim text-status-red',
  warning: 'border-status-orange/25 bg-status-orange-dim text-status-orange',
  success: 'border-status-green/25 bg-status-green-dim text-status-green',
} as const

type AlertVariant = keyof typeof VARIANT_CLASS

interface AlertBaseProps {
  variant: AlertVariant
  children: React.ReactNode
  className?: string
}

/**
 * A dismiss button is only rendered with an accessible name: `onDismiss` and
 * `dismissLabel` must be supplied together or not at all.
 */
type AlertProps = AlertBaseProps & (
  | { onDismiss?: undefined; dismissLabel?: undefined }
  | { onDismiss: () => void; dismissLabel: string }
)

export function Alert({ variant, children, onDismiss, dismissLabel, className = '' }: AlertProps) {
  const Icon = variant === 'success' ? CheckCircle2 : AlertTriangle
  return (
    <div
      // 'error' is assertive (role=alert); 'warning'/'success' are advisory
      // and use the polite role=status on purpose.
      role={variant === 'error' ? 'alert' : 'status'}
      className={`flex items-start gap-2.5 rounded-lg border px-3.5 py-2.5 text-sm ${VARIANT_CLASS[variant]} ${className}`}
    >
      <Icon className="size-4 shrink-0 mt-0.5" aria-hidden="true" />
      <span className="flex-1">{children}</span>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label={dismissLabel}
          className="shrink-0 opacity-70 hover:opacity-100"
        >
          ×
        </button>
      )}
    </div>
  )
}
