import type { Dashboard } from './types'

/** Port for the dashboard read. Hooks depend on this, never on the axios implementation. */
export interface IDashboardApi {
  getDashboard(spaceId: string): Promise<Dashboard>
}
