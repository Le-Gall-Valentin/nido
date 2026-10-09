import { useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { copyText } from '@/shared/lib/copyText'

export function ErrorFallback({ canRetry, onReset, error }: { canRetry: boolean; onReset: () => void; error: Error | null }) {
  const { t } = useTranslation('common')
  const btnRef = useRef<HTMLButtonElement>(null)
  const reason = error ? `${error.name}: ${error.message}` : null

  useEffect(() => {
    btnRef.current?.focus()
  }, [])

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-bg-0 px-4">
      <p role="alert" className="text-sm text-fg-2">{t('error.unexpected')}</p>
      {canRetry ? (
        <button
          ref={btnRef}
          type="button"
          className="mt-4 text-xs text-accent underline"
          onClick={onReset}
        >
          {t('error.retry')}
        </button>
      ) : (
        <button
          ref={btnRef}
          type="button"
          className="mt-4 text-xs text-accent underline"
          onClick={() => window.location.reload()}
        >
          {t('error.reload')}
        </button>
      )}

      {/*
        Collapsed on purpose: whoever is looking at this screen is told what to do first, and the
        reason is one click away for whoever ends up debugging it. It is here at all because a user
        cannot be asked to open devtools — with the name and the message on screen, a screenshot is
        enough to classify a crash that nothing else records.
      */}
      {reason && (
        <details className="mt-6 w-full max-w-md text-center">
          <summary className="cursor-pointer text-[11px] text-fg-3">{t('error.details')}</summary>
          <p className="mt-2 break-words font-mono text-[11px] text-fg-3">{reason}</p>
          <button
            type="button"
            className="mt-2 text-[11px] text-accent underline"
            onClick={() => void copyText(reason)}
          >
            {t('error.copy_details')}
          </button>
        </details>
      )}
    </main>
  )
}
