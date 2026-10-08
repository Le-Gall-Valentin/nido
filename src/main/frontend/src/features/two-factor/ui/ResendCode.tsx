import { useTranslation } from 'react-i18next'

interface ResendCodeProps {
  seconds: number
  onResend: () => void
  disabled?: boolean
}

function formatWait(seconds: number) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

/** "Pas reçu ?" — a link once the wait is over, the time left until then. */
export function ResendCode({ seconds, onResend, disabled = false }: ResendCodeProps) {
  const { t } = useTranslation('twoFactor')
  return (
    <p className="mt-3.5 text-center text-[13px] text-fg-2">
      {t('resend.question')}{' '}
      {seconds > 0 ? (
        <span className="text-fg-3 tabular-nums">{t('resend.wait', { time: formatWait(seconds) })}</span>
      ) : (
        <button
          type="button"
          onClick={onResend}
          disabled={disabled}
          className="bg-transparent border-0 p-0 font-semibold text-accent cursor-pointer hover:underline disabled:cursor-wait"
        >
          {t('resend.action')}
        </button>
      )}
    </p>
  )
}
