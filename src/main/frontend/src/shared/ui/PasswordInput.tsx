import { useState, type ComponentProps } from 'react'
import { useTranslation } from 'react-i18next'
import { Eye, EyeOff } from 'lucide-react'
import { Input } from './Input'
import { VERBATIM_INPUT_PROPS } from './verbatimInput'

type InputProps = ComponentProps<typeof Input>

interface PasswordInputProps extends Omit<InputProps, 'type' | 'suffix'> {
  /**
   * Whether the password shows, when a parent owns it — a confirmation field that follows this one
   * reads it too. Left out, the field keeps it to itself.
   */
  visible?: boolean
  onVisibleChange?: (visible: boolean) => void
}

/** A password field with the button that shows what was typed, and hides it again. */
export function PasswordInput({ visible, onVisibleChange, ...props }: PasswordInputProps) {
  const { t } = useTranslation('common')
  const [ownVisible, setOwnVisible] = useState(false)
  const shown = visible ?? ownVisible

  function toggle() {
    if (onVisibleChange) onVisibleChange(!shown)
    if (visible === undefined) setOwnVisible(!shown)
  }

  return (
    <Input
      {...VERBATIM_INPUT_PROPS}
      {...props}
      type={shown ? 'text' : 'password'}
      suffix={
        <button
          type="button"
          onClick={toggle}
          className="rounded-md p-2 text-fg-3 transition-colors hover:bg-bg-2 hover:text-fg-0"
          aria-label={shown ? t('password.hide') : t('password.show')}
        >
          {shown ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      }
    />
  )
}
