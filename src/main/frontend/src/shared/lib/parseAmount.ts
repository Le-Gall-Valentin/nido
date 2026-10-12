/** What can be wrong with a typed amount. */
export type AmountProblem = 'invalid' | 'too_precise' | 'too_large'

/** Twelve digits before the decimal point and two after: what the server keeps (`@Digits(integer = 12, fraction = 2)`). */
const MAX_INTEGER_DIGITS = 12
const MAX_DECIMALS = 2

const AMOUNT = /^(-?)(\d*)(?:\.(\d+))?$/

/**
 * An amount as a number field gives it — "12.5", ".5", "-150.5" where a negative one is allowed — or what is wrong
 * with it. It mirrors the server's limits, so that a refused amount is said here instead of coming back as a generic
 * error, which a retry would only repeat.
 */
export function parseAmount(input: string, { allowNegative = false }: { allowNegative?: boolean } = {}):
  { value: number } | { problem: AmountProblem } {
  const typed = input.trim()
  const match = AMOUNT.exec(typed)
  const sign = match?.[1] ?? ''
  const integer = match?.[2] ?? ''
  const decimals = match?.[3] ?? ''
  if (!match || (sign && !allowNegative) || (integer === '' && decimals === '')) return { problem: 'invalid' }
  if (decimals.length > MAX_DECIMALS) return { problem: 'too_precise' }
  if (integer.replace(/^0+(?=\d)/, '').length > MAX_INTEGER_DIGITS) return { problem: 'too_large' }
  return { value: Number(typed) }
}
