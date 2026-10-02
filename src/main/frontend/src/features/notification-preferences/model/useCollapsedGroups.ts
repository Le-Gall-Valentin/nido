import { useState } from 'react'
import { readCollapsedGroups, writeCollapsedGroups } from './collapsedGroups'

/** Which categories of the card are folded, read once from this device and written back on every fold. */
export function useCollapsedGroups() {
  const [collapsed, setCollapsed] = useState<ReadonlySet<string>>(readCollapsedGroups)

  function toggle(group: string) {
    setCollapsed((current) => {
      const next = new Set(current)
      if (next.has(group)) next.delete(group)
      else next.add(group)
      writeCollapsedGroups(next)
      return next
    })
  }

  return { isCollapsed: (group: string) => collapsed.has(group), toggle }
}
