import { create } from 'zustand'

export const LAST_SPACE_STORAGE_KEY = 'nido:lastSpace'

interface ActiveSpaceState {
  lastSpaceId: string | null
  remember: (spaceId: string) => void
  forget: () => void
}

// localStorage can throw (private browsing, blocked storage). A context
// switch must keep working even when the browser refuses to persist it, so
// every read and write is wrapped and failures are swallowed.

function readStoredSpaceId(): string | null {
  try {
    return localStorage.getItem(LAST_SPACE_STORAGE_KEY)
  } catch {
    return null
  }
}

function writeStoredSpaceId(spaceId: string): void {
  try {
    localStorage.setItem(LAST_SPACE_STORAGE_KEY, spaceId)
  } catch {
    // Storage denied: the in-memory state below still updates.
  }
}

function clearStoredSpaceId(): void {
  try {
    localStorage.removeItem(LAST_SPACE_STORAGE_KEY)
  } catch {
    // Storage denied: the in-memory state below still updates.
  }
}

/**
 * Remembers the last context the user chose, so navigation can be restored
 * on an unscoped URL. This is the only thing it does: the active context
 * itself always comes from the URL (see useActiveSpace), never from here.
 */
export const activeSpaceStore = create<ActiveSpaceState>((set) => ({
  lastSpaceId: readStoredSpaceId(),

  remember(spaceId: string): void {
    writeStoredSpaceId(spaceId)
    set({ lastSpaceId: spaceId })
  },

  forget(): void {
    clearStoredSpaceId()
    set({ lastSpaceId: null })
  },
}))
