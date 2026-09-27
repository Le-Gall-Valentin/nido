import { createContext, useContext, type ReactNode } from 'react'

/** A debt the caller is about to settle, opened from "À traiter" or from the finance card. */
export interface PendingSettlement {
  toMemberId: string
  amount: number
}

/**
 * What every block needs from the page to act: the space, whether the caller may write, who the caller
 * is, how to name a member, and the page-level flows. One context instead of the same props threaded
 * through every card and row.
 *
 * Where an action is wired — the rule every new one follows:
 * - A flow several blocks open is the page's, reached through this context: settling a debt (from
 *   "À traiter" and from the finance card — one dialog, whichever opened it), and reporting a failed
 *   action (from any row — one alert above the board).
 * - What a single block opens stays with it: the menu card's "send the week to the shopping list"
 *   dialog is the menu card's, adding a task is handed to the page's own header as a prop.
 * - A row that writes calls its entity's hook itself (ticking a task, accepting an invitation) and says
 *   a failure through `reportError`.
 */
export interface DashboardActions {
  spaceId: string
  canWrite: boolean
  isShared: boolean
  currentUserId: string | null
  /** The member's username, or "Ancien membre" when the page cannot name them. */
  memberName: (memberId: string) => string
  settle: (debt: PendingSettlement) => void
  reportError: () => void
}

const DashboardActionsContext = createContext<DashboardActions | null>(null)

export function DashboardActionsProvider({ value, children }: { value: DashboardActions; children: ReactNode }) {
  return <DashboardActionsContext.Provider value={value}>{children}</DashboardActionsContext.Provider>
}

export function useDashboardActions(): DashboardActions {
  const actions = useContext(DashboardActionsContext)
  if (!actions) {
    throw new Error('useDashboardActions must be used within a DashboardActionsProvider')
  }
  return actions
}
