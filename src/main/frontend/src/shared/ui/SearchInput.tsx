import { Search, X } from 'lucide-react'

interface SearchInputProps {
  value: string
  onChange: (value: string) => void
  placeholder: string
  /** Accessible name for the input; falls back to the placeholder. */
  label?: string
  /** Accessible name for the clear button; the button only renders when provided. */
  clearLabel?: string
  className?: string
}

export function SearchInput({ value, onChange, placeholder, label, clearLabel, className = '' }: SearchInputProps) {
  return (
    <div className={`relative ${className}`}>
      <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-3.5 text-fg-3 pointer-events-none" aria-hidden="true" />
      <input
        type="search"
        aria-label={label ?? placeholder}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-border bg-bg-1 pl-9 pr-9 py-2 text-sm text-fg-0 outline-none placeholder:text-fg-3 transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] [&::-webkit-search-cancel-button]:hidden"
      />
      {clearLabel && value.length > 0 && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label={clearLabel}
          className="absolute right-2 top-1/2 -translate-y-1/2 size-5 flex items-center justify-center rounded text-fg-3 hover:text-fg-0 hover:bg-bg-3"
        >
          <X className="size-3.5" />
        </button>
      )}
    </div>
  )
}
