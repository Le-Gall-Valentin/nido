import { isPersonal, safeAccent } from '@/entities/space'
import { useActiveSpace } from '@/features/space-switcher'

/**
 * A 3px strip in the active space's accent colour, shown only while the
 * current page belongs to a shared group — a quick visual reminder that
 * you're looking at group content, not your personal space. Sanitises the
 * accent itself via safeAccent: SpaceLayout's --space-accent lives on a
 * div inside the routed <main> content, a separate subtree this component
 * (rendered above <main>, next to Topbar) has no CSS access to.
 */
export function GroupAccentStrip() {
  const { space } = useActiveSpace()
  if (!space || isPersonal(space)) return null

  return <div data-testid="group-accent-strip" className="h-[3px] shrink-0" style={{ background: safeAccent(space.accent) }} />
}
