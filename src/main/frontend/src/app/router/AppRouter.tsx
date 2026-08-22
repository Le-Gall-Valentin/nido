import { lazy } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { ROUTES } from '@/shared/config'
import { AppLayout, SpaceLayout } from '@/app/layouts'
import { SpacesApiProvider, spacesApi } from '@/features/space-switcher'
import { ProtectedRoute } from './ProtectedRoute'
import { AdminRoute } from './AdminRoute'
import { SpaceRoute } from './SpaceRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { DefaultRedirect } from './DefaultRedirect'

const AdminUsersPage = lazy(() => import('@/pages/admin-users'))
const AccountPage = lazy(() => import('@/pages/account'))

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route
          path={ROUTES.LOGIN}
          element={
            <PublicOnlyRoute>
              <LoginPage />
            </PublicOnlyRoute>
          }
        />

        {/* Protected — shell layout wraps all authenticated pages */}
        <Route
          element={
            <ProtectedRoute>
              <SpacesApiProvider api={spacesApi}>
                <AppLayout />
              </SpacesApiProvider>
            </ProtectedRoute>
          }
        >
          {/* Section redirect */}
          <Route path={ROUTES.ADMINISTRATION} element={<Navigate to={ROUTES.ADMIN_USERS} replace />} />

          {/* Administration (admin-only) */}
          <Route element={<AdminRoute><Outlet /></AdminRoute>}>
            <Route path={ROUTES.ADMIN_USERS} element={<AdminUsersPage />} />
          </Route>

          <Route path={ROUTES.ACCOUNT} element={<AccountPage />} />

          {/* Scoped context — Task 3 registers the members child route and the
              /spaces groups page; until then the outlet below is empty. */}
          <Route
            path="/s/:spaceId"
            element={
              <SpaceRoute>
                <SpaceLayout />
              </SpaceRoute>
            }
          />
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<DefaultRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}