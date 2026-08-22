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
const SpacesPage = lazy(() => import('@/pages/spaces'))
const SpaceMembersPage = lazy(() => import('@/pages/spaces').then((m) => ({ default: m.SpaceMembersPage })))

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

          <Route path={ROUTES.SPACES} element={<SpacesPage />} />

          {/* Scoped context subtree: the guard resolves the caller's contexts
              before anything renders, and the layout carries the context's
              accent down to the pages mounted under its outlet. The index
              route redirects to `members` so a context switch from the
              topbar always lands on a real page, never an empty outlet. */}
          <Route
            path={ROUTES.space(':spaceId')}
            element={
              <SpaceRoute>
                <SpaceLayout />
              </SpaceRoute>
            }
          >
            <Route index element={<Navigate to="members" replace />} />
            <Route path="members" element={<SpaceMembersPage />} />
          </Route>
        </Route>

        {/* Catch-all — needs its own space list to restore the last context */}
        <Route
          path="*"
          element={
            <SpacesApiProvider api={spacesApi}>
              <DefaultRedirect />
            </SpacesApiProvider>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}