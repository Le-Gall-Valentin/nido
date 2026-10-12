import { describe, expect, it } from 'vitest'
import { parseAmount } from './parseAmount'

describe('parseAmount', () => {
  it('reads an amount as a number input gives it, a leading zero left out included', () => {
    expect(parseAmount('12.5')).toEqual({ value: 12.5 })
    expect(parseAmount(' 7 ')).toEqual({ value: 7 })
    expect(parseAmount('.5')).toEqual({ value: 0.5 })
    expect(parseAmount('0.05')).toEqual({ value: 0.05 })
  })

  it('reads a negative amount only where one is allowed', () => {
    expect(parseAmount('-150.5', { allowNegative: true })).toEqual({ value: -150.5 })
    expect(parseAmount('-.5', { allowNegative: true })).toEqual({ value: -0.5 })
    expect(parseAmount('-150.5')).toEqual({ problem: 'invalid' })
  })

  it('says what is wrong with what is not an amount', () => {
    expect(parseAmount('')).toEqual({ problem: 'invalid' })
    expect(parseAmount('-', { allowNegative: true })).toEqual({ problem: 'invalid' })
    expect(parseAmount('.')).toEqual({ problem: 'invalid' })
    expect(parseAmount('1e3')).toEqual({ problem: 'invalid' })
    expect(parseAmount('12.345')).toEqual({ problem: 'too_precise' })
  })

  it("refuses more digits than the server keeps: twelve before the decimal point", () => {
    expect(parseAmount('999999999999.99')).toEqual({ value: 999999999999.99 })
    expect(parseAmount('1000000000000')).toEqual({ problem: 'too_large' })
  })
})
