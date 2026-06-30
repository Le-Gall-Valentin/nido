import React, { forwardRef, useImperativeHandle, useRef } from 'react'

export interface TotpDigitInputHandle {
  focusFirst: () => void
}

interface TotpDigitInputProps {
  value: string
  onChange: (v: string) => void
  disabled?: boolean
  autoFocus?: boolean
  groupLabel?: string
  digitLabel?: (index: number) => string
}

export const TotpDigitInput = forwardRef<TotpDigitInputHandle, TotpDigitInputProps>(
  function TotpDigitInput(
    {
      value,
      onChange,
      disabled = false,
      autoFocus = false,
      groupLabel = 'Verification code',
      digitLabel = (i) => `Digit ${i + 1}`,
    },
    ref
  ) {
    const inputs = useRef<(HTMLInputElement | null)[]>([])

    useImperativeHandle(ref, () => ({
      focusFirst: () => inputs.current[0]?.focus(),
    }))

    const digits = Array.from({ length: 6 }, (_, i) => value[i] ?? '')

    function setDigit(index: number, char: string) {
      const digit = char.replace(/\D/g, '').slice(-1)
      const next = [...digits]
      next[index] = digit
      onChange(next.join(''))
      if (digit && index < 5) inputs.current[index + 1]?.focus()
    }

    function handleKeyDown(index: number, e: React.KeyboardEvent<HTMLInputElement>) {
      if (e.key === 'Backspace' && !digits[index] && index > 0) {
        const next = [...digits]
        next[index - 1] = ''
        onChange(next.join(''))
        inputs.current[index - 1]?.focus()
      } else if (e.key === 'ArrowLeft' && index > 0) {
        inputs.current[index - 1]?.focus()
      } else if (e.key === 'ArrowRight' && index < 5) {
        inputs.current[index + 1]?.focus()
      }
    }

    function handlePaste(e: React.ClipboardEvent) {
      e.preventDefault()
      const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
      if (!text) return
      const next = Array.from({ length: 6 }, (_, i) => text[i] ?? '')
      onChange(next.join(''))
      inputs.current[Math.min(text.length, 5)]?.focus()
    }

    return (
      <div
        role="group"
        aria-label={groupLabel}
        className="flex gap-2 my-2"
        onPaste={handlePaste}
      >
        {digits.map((d, i) => (
          <input
            key={i}
            ref={el => { inputs.current[i] = el }}
            type="text"
            inputMode="numeric"
            maxLength={1}
            value={d}
            disabled={disabled}
            autoFocus={autoFocus && i === 0}
            autoComplete={i === 0 ? 'one-time-code' : 'off'}
            aria-label={digitLabel(i)}
            className="flex-1 min-w-0 h-14 bg-bg-2 border border-border-2 rounded-lg text-fg-0 text-center font-mono text-[22px] font-semibold outline-none transition-colors focus:border-accent focus:bg-bg-3 disabled:opacity-50"
            onChange={e => setDigit(i, e.target.value)}
            onKeyDown={e => handleKeyDown(i, e)}
          />
        ))}
      </div>
    )
  }
)