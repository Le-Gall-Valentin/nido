import { Component, useRef, useEffect, type ErrorInfo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

const MAX_RETRIES = 2

function ErrorFallback({ canRetry, onReset }: { canRetry: boolean; onReset: () => void }) {
  const { t } = useTranslation('common')
  const btnRef = useRef<HTMLButtonElement>(null)

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
    </main>
  )
}

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  retryCount: number
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, retryCount: 0 }

  static getDerivedStateFromError(): Partial<State> {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    if (import.meta.env.DEV) {
      console.error('[ErrorBoundary]', error, info.componentStack)
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <ErrorFallback
          canRetry={this.state.retryCount < MAX_RETRIES}
          onReset={() => this.setState(s => ({ hasError: false, retryCount: s.retryCount + 1 }))}
        />
      )
    }
    return this.props.children
  }
}