import { useState } from 'react'
import { readCollapsedGroups, writeCollapsedGroups } from './collapsedGroups'

/** Which categories of the card are folded, read once from this device and written back on every fold. */
export function useCollapsedGroups() {
  const [collapsed, setCollapsed] = useState<ReadonlySet<string>>(readCollapsedGroups)

  function toggle(group: string) {
    const next = new Set(collapsed)
    if (next.has(group)) next.delete(group)
    else next.add(group)
    // Written here, in the click: a state update must stay pure, React may run it twice.
    writeCollapsedGroups(next)
    setCollapsed(next)
  }

  return { isCollapsed: (group: string) => collapsed.has(group), toggle }
}
