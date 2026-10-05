import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { shouldRetryQuery } from '@/shared/api'
import { AccountLanguageSync } from '@/features/auth'
import { ThemeProvider, LanguageProvider, AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'
import { SetupGate } from './router/SetupGate'

const queryClient = new QueryClient({
  defaultOptions: {
    // retry was 1 flat, which retried the statuses a second attempt cannot change — 401 above all,
    // where the interceptor had already refreshed and replayed. See shouldRetryQuery.
    queries: { staleTime: 30_000, retry: shouldRetryQuery, refetchOnWindowFocus: false },
    mutations: { retry: 0 },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <LanguageProvider>
          <SetupGate>
            <AuthProvider>
              <AccountLanguageSync />
              <ErrorBoundary>
                <AppRouter />
              </ErrorBoundary>
            </AuthProvider>
          </SetupGate>
        </LanguageProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
