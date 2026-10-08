import React, { forwardRef, useImperativeHandle, useRef, useState } from 'react'
import { twMerge } from 'tailwind-merge'

export interface CodeInputHandle {
  focus: () => void
}

interface CodeInputProps {
  value: string
  onChange: (v: string) => void
  disabled?: boolean
  autoFocus?: boolean
  label?: string
}

const CODE_LENGTH = 6

function digitsOf(text: string) {
  return text.replace(/\D/g, '').slice(0, CODE_LENGTH)
}

/**
 * One real field, drawn as six boxes. Password managers, the phone keyboard's code suggestion and
 * paste all write the whole code into the `one-time-code` field: with one field per digit, only the
 * first box was filled. The field lies on top of the boxes with its text made transparent — not the
 * field itself: Bitwarden skips a field under 0.1 opacity or covered by another element, and iOS
 * only offers to paste into an opaque one.
 */
export const CodeInput = forwardRef<CodeInputHandle, CodeInputProps>(
  function CodeInput(
    { value, onChange, disabled = false, autoFocus = false, label = 'Verification code' },
    ref
  ) {
    const input = useRef<HTMLInputElement>(null)
    const [focused, setFocused] = useState(false)

    useImperativeHandle(ref, () => ({
      focus: () => input.current?.focus(),
    }))

    const next = Math.min(value.length, CODE_LENGTH - 1)

    // A paste is the whole code: it replaces what was typed rather than landing after it.
    function handlePaste(e: React.ClipboardEvent<HTMLInputElement>) {
      e.preventDefault()
      const code = digitsOf(e.clipboardData.getData('text'))
      if (code) onChange(code)
    }

    // The caret stays after the last digit, in the highlighted box: a click on an earlier box
    // would otherwise slip the next digit in between, out of sight.
    function keepCaretAtEnd(e: React.SyntheticEvent<HTMLInputElement>) {
      const field = e.currentTarget
      const end = field.value.length
      if (field.selectionStart !== end || field.selectionEnd !== end) field.setSelectionRange(end, end)
    }

    return (
      <div className="relative my-2">
        <div aria-hidden="true" className="flex gap-[9px]">
          {Array.from({ length: CODE_LENGTH }, (_, i) => (
            <div
              key={i}
              className={twMerge(
                'flex flex-1 min-w-0 aspect-square max-h-14 items-center justify-center bg-bg-1 border-[1.5px] border-border rounded-[12px] text-fg-0 text-[22px] font-semibold transition-colors',
                focused && i === next && 'border-accent',
                disabled && 'opacity-50'
              )}
            >
              {value[i] ?? (focused && i === next && <span className="h-6 w-px bg-fg-0 motion-safe:animate-pulse" />)}
            </div>
          ))}
        </div>
        <input
          ref={input}
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          value={value}
          disabled={disabled}
          autoFocus={autoFocus}
          aria-label={label}
          className="absolute inset-0 size-full bg-transparent text-[22px] text-transparent caret-transparent outline-none selection:bg-transparent autofill:opacity-0 disabled:cursor-not-allowed"
          onChange={e => onChange(digitsOf(e.target.value))}
          onPaste={handlePaste}
          onSelect={keepCaretAtEnd}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
        />
      </div>
    )
  }
)
