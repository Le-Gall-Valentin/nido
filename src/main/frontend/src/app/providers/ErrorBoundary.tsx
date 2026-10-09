import { Component, type ErrorInfo, type ReactNode } from 'react'
import { ErrorFallback } from './ErrorFallback'

const MAX_RETRIES = 2

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