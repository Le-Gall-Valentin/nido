import { render, screen } from '@testing-library/react'
import { Input } from './Input'
import { describe, it, expect } from 'vitest'

describe('Input', () => {
  it('renders with label', () => {
    render(<Input label="Email" id="email" />)
    expect(screen.getByLabelText('Email')).not.toBeNull()
  })

  it('passes through input attributes', () => {
    render(<Input label="Username" id="username" placeholder="Enter username" type="text" />)
    const input = screen.getByLabelText('Username') as HTMLInputElement
    expect(input.placeholder).toBe('Enter username')
    expect(input.type).toBe('text')
  })

  it('renders suffix when provided', () => {
    render(<Input label="Password" id="password" suffix={<span>toggle</span>} />)
    expect(screen.getByText('toggle')).not.toBeNull()
  })

  it('keeps the label accessible but visually hidden when srOnlyLabel is set', () => {
    render(<Input label="Ingredient name" id="ingredient" srOnlyLabel />)
    const label = screen.getByText('Ingredient name')
    expect(label.className).toContain('sr-only')
    expect(screen.getByLabelText('Ingredient name')).not.toBeNull()
  })
})