import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { SettingInput } from './SettingInput'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }))

describe('SettingInput', () => {
  it('offers the protections in a list', () => {
    const onChange = vi.fn()
    render(<SettingInput settingKey="mail.security" value="starttls" onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('field.mail.security'), { target: { value: 'tls' } })

    expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['option.starttls', 'option.tls', 'option.none'])
    expect(onChange).toHaveBeenCalledWith('tls')
  })

  it('turns a flag on and off with a switch', () => {
    const onChange = vi.fn()
    render(<SettingInput settingKey="api.swagger" value="false" onChange={onChange} />)

    fireEvent.click(screen.getByRole('switch', { name: 'field.api.swagger' }))

    expect(onChange).toHaveBeenCalledWith('true')
  })

  it('keeps password managers away from the fields, and asks numbers for a number', () => {
    render(<>
      <SettingInput settingKey="mail.password" value="" onChange={vi.fn()} />
      <SettingInput settingKey="mail.port" value="587" onChange={vi.fn()} />
      <SettingInput settingKey="a.setting.of.a.later.version" value="" onChange={vi.fn()} />
    </>)

    expect(screen.getByLabelText('field.mail.password').getAttribute('autocomplete')).toBe('new-password')
    expect(screen.getByLabelText('field.mail.port').getAttribute('inputmode')).toBe('numeric')
    expect(screen.getByLabelText('field.a.setting.of.a.later.version').getAttribute('autocomplete')).toBe('off')
  })
})
