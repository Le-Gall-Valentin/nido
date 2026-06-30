import { Loader2 } from 'lucide-react'

export function Spinner({ label = 'Loading', fullscreen = true }: { label?: string; fullscreen?: boolean } = {}) {
  return (
    <div
      className={`flex items-center justify-center bg-bg-0 ${fullscreen ? 'min-h-screen' : ''}`}
      role="status"
      aria-label={label}
    >
      <Loader2 className="size-8 animate-spin text-accent" aria-hidden="true" />
    </div>
  )
}