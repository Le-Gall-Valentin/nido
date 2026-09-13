import { useEffect, useRef, useState } from 'react'
import { useDebouncedValue } from '@/shared/lib'
import { SearchInput } from './SearchInput'

interface DebouncedSearchInputProps {
  /** Called with the settled value only — never on every keystroke. */
  onSearch: (value: string) => void
  placeholder: string
  clearLabel: string
  delayMs?: number
  className?: string
}

/**
 * A search field that keeps what is being typed to itself and reports only the settled value.
 *
 * <p>The point is what does *not* re-render. Held by the page, the raw input meant one page render per
 * keystroke, and the page renders its list: on the users screen, measured, five characters cost five
 * renders of a twenty-row table — a hundred rows of markup to type "alice". Here the keystrokes stop at
 * this component, and the page hears from it once the typing settles.
 *
 * <p>Cheaper than memoising the table, and nothing to keep in sync: there is no second half of the fix
 * that a later refactor can quietly undo.
 */
export function DebouncedSearchInput({
  onSearch, placeholder, clearLabel, delayMs = 300, className,
}: DebouncedSearchInputProps) {
  const [value, setValue] = useState('')
  const settled = useDebouncedValue(value, delayMs)
  const lastReported = useRef(settled)

  useEffect(() => {
    if (settled === lastReported.current) return
    lastReported.current = settled
    onSearch(settled)
  }, [settled, onSearch])

  return (
    <SearchInput
      value={value}
      onChange={setValue}
      placeholder={placeholder}
      clearLabel={clearLabel}
      className={className}
    />
  )
}
