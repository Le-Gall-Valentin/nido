import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { SetupPage, setupApi, useSetupStatus, type ISetupApi, type SetupStatus } from '@/pages/setup'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { ErrorBoundary } from '../providers/ErrorBoundary'

interface Props {
  children: ReactNode
  api?: ISetupApi
  /** Composition seam: tests render a marker instead of the whole wizard. */
  renderSetup?: (status: SetupStatus) => ReactNode
}

/**
 * Before anything else, including the session: an installation not set up yet shows its setup screen
 * at /setup, and every other address leads there. Once set up, the application renders as before.
 */
export function SetupGate({ children, api = setupApi, renderSetup = (status) => <SetupPage status={status} /> }: Props) {
  const { t } = useTranslation('common')
  const setup = useSetupStatus(api)
  if (setup.state === 'loading') return <Spinner label={t('loading')} />
  if (setup.state === 'required') {
    return (
      <BrowserRouter>
        <Routes>
          {/* Outside the application's own boundary, which sits under the session: without one, a crash here is a blank page. */}
          <Route path={ROUTES.SETUP} element={<ErrorBoundary>{renderSetup(setup.status)}</ErrorBoundary>} />
          <Route path="*" element={<Navigate to={ROUTES.SETUP} replace />} />
        </Routes>
      </BrowserRouter>
    )
  }
  return <>{children}</>
}
