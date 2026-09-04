import { lazy } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { LoginPage } from '@/pages/login'
import { ROUTES } from '@/shared/config'
import { AppLayout, SpaceLayout } from '@/app/layouts'
import { SpaceMembersApiProvider } from '@/entities/space'
import { TasksApiProvider, tasksApi } from '@/entities/tasks'
import { SpacesApiProvider, spacesApi } from '@/features/space-switcher'
import { SpacesPageApiProvider, spacesPageApi } from '@/pages/spaces/invitations'
import { ProtectedRoute } from './ProtectedRoute'
import { AdminRoute } from './AdminRoute'
import { SpaceRoute } from './SpaceRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'
import { DefaultRedirect } from './DefaultRedirect'

const AdminUsersPage = lazy(() => import('@/pages/admin-users'))
const AccountProfilePage = lazy(() => import('@/pages/account'))
const AccountSecurityPage = lazy(() => import('@/pages/account').then((m) => ({ default: m.AccountSecurityPage })))
const AccountPreferencesPage = lazy(() => import('@/pages/account').then((m) => ({ default: m.AccountPreferencesPage })))
const SpacesPage = lazy(() => import('@/pages/spaces'))
const SpaceMembersPage = lazy(() => import('@/pages/spaces').then((m) => ({ default: m.SpaceMembersPage })))
const KitchenRecipesPage = lazy(() => import('@/pages/kitchen'))
const KitchenRecipeDetailPage = lazy(() =>
  import('@/pages/kitchen').then((m) => ({ default: m.KitchenRecipeDetailPage })))
const KitchenMenuPage = lazy(() => import('@/pages/kitchen').then((m) => ({ default: m.KitchenMenuPage })))
const ShoppingListPage = lazy(() => import('@/pages/shopping'))
const TasksPage = lazy(() => import('@/pages/tasks'))

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
                <SpacesPageApiProvider api={spacesPageApi}>
                  <SpaceMembersApiProvider api={spacesPageApi}>
                    <TasksApiProvider api={tasksApi}>
                      <AppLayout />
                    </TasksApiProvider>
                  </SpaceMembersApiProvider>
                </SpacesPageApiProvider>
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

          <Route path={ROUTES.ACCOUNT} element={<Outlet />}>
            <Route index element={<Navigate to="profile" replace />} />
            <Route path="profile" element={<AccountProfilePage />} />
            <Route path="security" element={<AccountSecurityPage />} />
            <Route path="preferences" element={<AccountPreferencesPage />} />
          </Route>

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
            <Route path="kitchen">
              <Route index element={<Navigate to="recipes" replace />} />
              <Route path="recipes" element={<KitchenRecipesPage />} />
              <Route path="recipes/:recipeId" element={<KitchenRecipeDetailPage />} />
              <Route path="menu" element={<KitchenMenuPage />} />
            </Route>
            <Route path="organisation">
              <Route index element={<Navigate to="courses" replace />} />
              <Route path="courses" element={<ShoppingListPage />} />
              <Route path="tasks" element={<TasksPage />} />
            </Route>
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