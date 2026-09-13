import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { shouldRetryQuery } from '@/shared/api'
import { ThemeProvider, LanguageProvider, AuthProvider, ErrorBoundary } from './providers'
import { AppRouter } from './router'

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
          <AuthProvider>
            <ErrorBoundary>
              <AppRouter />
            </ErrorBoundary>
          </AuthProvider>
        </LanguageProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
