import { createContext, useContext, type ReactNode } from 'react'

/** A debt the caller is about to settle, opened from "À traiter" or from the finance card. */
export interface PendingSettlement {
  toMemberId: string
  amount: number
}

/**
 * What every block needs from the page to act: the space, whether the caller may write, who the caller
 * is, how to name a member, and the two page-level flows (settling a debt, reporting a failed action).
 * One context instead of the same six props threaded through every card and row.
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
