import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { TasksApiProvider, type TasksApi } from '@/entities/tasks'
import { SpaceApiProvider, type ISpaceApi } from '@/entities/space'
import { KitchenApiProvider, type IKitchenApi } from '@/entities/kitchen'
import { DashboardActionsProvider } from '../model/DashboardActionsProvider'
import type { DashboardActions } from '../model/dashboardActions'

/**
 * Renders one dashboard block the way the page mounts it — router, query client, the APIs the blocks
 * write through, and the actions context — for the component tests of this page. Callers pass
 * `vi.fn()`s for whatever they assert on; everything else is a harmless no-op.
 *
 * Test code only: it lives in this page's `test/` segment, out of `ui/`, and out of the coverage figure
 * (vitest.config.ts), as the shared harness in `shared/test` does.
 */
const NAMES: Record<string, string> = { 'u-me': 'valentin', 'u-cam': 'camille', 'u-paul': 'paul', 'u-lea': 'lea' }

export function baseActions(overrides: Partial<DashboardActions> = {}): DashboardActions {
  return {
    spaceId: 'space-1',
    canWrite: true,
    isShared: true,
    currentUserId: 'u-me',
    memberName: (id) => NAMES[id] ?? 'member.former',
    settle: () => {},
    reportError: () => {},
    ...overrides,
  }
}

interface HarnessOptions {
  actions?: Partial<DashboardActions>
  tasksApi?: Partial<TasksApi>
  spaceApi?: Partial<ISpaceApi>
  kitchenApi?: Partial<IKitchenApi>
}

export function renderWithActions(ui: ReactElement, options: HarnessOptions = {}) {
  const actions = baseActions(options.actions)
  const tasksApi = { changeTaskStatus: () => Promise.resolve(undefined), ...options.tasksApi } as unknown as TasksApi
  const spaceApi = { acceptInvitation: () => Promise.resolve({ spaceId: 'space-9' }), ...options.spaceApi } as unknown as ISpaceApi
  const kitchenApi = { getShoppingList: () => Promise.resolve([]), ...options.kitchenApi } as unknown as IKitchenApi
  const view = render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SpaceApiProvider api={spaceApi}>
        <TasksApiProvider api={tasksApi}>
          <KitchenApiProvider api={kitchenApi}>
            <MemoryRouter>
              <DashboardActionsProvider value={actions}>{ui}</DashboardActionsProvider>
            </MemoryRouter>
          </KitchenApiProvider>
        </TasksApiProvider>
      </SpaceApiProvider>
    </QueryClientProvider>
  )
  return { ...view, actions, tasksApi, spaceApi, kitchenApi }
}
