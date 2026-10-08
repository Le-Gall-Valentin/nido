import { ChevronRight, Mail, Smartphone } from 'lucide-react'
import type { TwoFactorMethod } from '@/entities/user'

interface MethodCardProps {
  method: TwoFactorMethod
  title: string
  description: string
  tag?: string
  onSelect: () => void
  disabled?: boolean
}

/** A method to pick — at sign-in when both are on, and to turn one on. The whole card is the button. */
export function MethodCard({ method, title, description, tag, onSelect, disabled = false }: MethodCardProps) {
  const Icon = method === 'APP' ? Smartphone : Mail
  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={disabled}
      className="grid w-full grid-cols-[38px_minmax(0,1fr)_16px] items-center gap-3 rounded-[14px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[13px] text-left text-fg-0 transition-colors hover:border-accent focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-wait disabled:opacity-60"
    >
      <span className="grid size-[38px] place-items-center rounded-[10px] bg-accent-dim text-accent">
        <Icon className="size-[18px]" aria-hidden="true" />
      </span>
      <span className="min-w-0">
        <span className="flex flex-wrap items-center gap-[7px] text-sm font-semibold">
          {title}
          {tag && (
            <span className="rounded-[5px] bg-status-green-dim px-1.5 py-[3px] text-[10px] font-bold uppercase tracking-[0.05em] text-status-green">
              {tag}
            </span>
          )}
        </span>
        <span className="mt-px block text-[12.5px] leading-snug text-fg-2">{description}</span>
      </span>
      <ChevronRight className="size-4 text-fg-3" aria-hidden="true" />
    </button>
  )
}
