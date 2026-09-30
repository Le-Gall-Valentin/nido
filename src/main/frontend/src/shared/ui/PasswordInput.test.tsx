import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PasswordInput } from './PasswordInput'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const field = (label: string) => screen.getByLabelText(label) as HTMLInputElement

describe('PasswordInput', () => {
  it('hides the password until asked, and says what its button does next', () => {
    render(<PasswordInput label="Password" name="password" value="secret" onChange={() => {}} />)

    expect(field('Password').type).toBe('password')
    fireEvent.click(screen.getByRole('button', { name: 'password.show' }))
    expect(field('Password').type).toBe('text')
    fireEvent.click(screen.getByRole('button', { name: 'password.hide' }))
    expect(field('Password').type).toBe('password')
  })

  it('follows a visibility its parent owns, so a confirmation field can follow it too', () => {
    function Harness() {
      const [visible, setVisible] = useState(false)
      return (
        <>
          <PasswordInput label="New" name="new" value="" onChange={() => {}} visible={visible} onVisibleChange={setVisible} />
          <input aria-label="Confirm" type={visible ? 'text' : 'password'} readOnly />
        </>
      )
    }
    render(<Harness />)

    fireEvent.click(screen.getByRole('button', { name: 'password.show' }))

    expect(field('New').type).toBe('text')
    expect(field('Confirm').type).toBe('text')
  })

  it('passes the field attributes through', () => {
    render(
      <PasswordInput label="Password" name="password" value="" onChange={() => {}}
        autoComplete="new-password" aria-describedby="rules" aria-invalid />,
    )

    expect(field('Password').getAttribute('autocomplete')).toBe('new-password')
    expect(field('Password').getAttribute('aria-describedby')).toBe('rules')
    expect(field('Password').getAttribute('aria-invalid')).toBe('true')
    expect(field('Password').getAttribute('spellcheck')).toBe('false')
  })
})
