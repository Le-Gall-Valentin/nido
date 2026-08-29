import { create } from 'zustand'

const STORAGE_KEY = 'nido:sidebarCollapsed'

interface SidebarCollapseState {
  collapsed: boolean
  toggle: () => void
}

// localStorage can throw (private browsing, blocked storage) — every read
// and write is wrapped and failures are swallowed, same convention as
// activeSpaceStore: a persistence failure must never break the toggle.

function readStoredCollapsed(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

function writeStoredCollapsed(collapsed: boolean): void {
  try {
    localStorage.setItem(STORAGE_KEY, String(collapsed))
  } catch {
    // Storage denied: the in-memory state below still updates.
  }
}

/** Whether the desktop sidebar is collapsed to its icon-only rail — a pure display preference, remembered across sessions. */
export const sidebarCollapseStore = create<SidebarCollapseState>((set, get) => ({
  collapsed: readStoredCollapsed(),

  toggle(): void {
    const next = !get().collapsed
    writeStoredCollapsed(next)
    set({ collapsed: next })
  },
}))
