import { Component, useRef, useEffect, type ErrorInfo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

const MAX_RETRIES = 2

function ErrorFallback({ canRetry, onReset, error }: { canRetry: boolean; onReset: () => void; error: Error | null }) {
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
            onClick={() => void navigator.clipboard?.writeText(reason)}
          >
            {t('error.copy_details')}
          </button>
        </details>
      )}
    </main>
  )
}

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  retryCount: number
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, retryCount: 0, error: null }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error }
  }

  /**
   * Logged in every build, not only in a dev one. A crash on a user's machine used to leave nothing
   * behind — no name, no message, nothing to ask them for — which is how one stayed undiagnosed long
   * enough for the people hitting it to settle on using the application in a private window instead.
   */
  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return (
        <ErrorFallback
          canRetry={this.state.retryCount < MAX_RETRIES}
          error={this.state.error}
          onReset={() => this.setState(s => ({ hasError: false, retryCount: s.retryCount + 1, error: null }))}
        />
      )
    }
    return this.props.children
  }
}