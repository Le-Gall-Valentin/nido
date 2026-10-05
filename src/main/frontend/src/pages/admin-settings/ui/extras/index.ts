import type { SettingsGroupCode } from '@/entities/instance-settings'
import { MailActions } from './MailActions'
import { MailNotes } from './MailNotes'
import { PublicUrlNotes } from './PublicUrlNotes'
import type { GroupExtras } from './types'

/** A block with something of its own adds it here; the card stays the same for all. */
export const GROUP_EXTRAS: Partial<Record<SettingsGroupCode, GroupExtras>> = {
  mail: { Notes: MailNotes, Actions: MailActions },
  'public-url': { Notes: PublicUrlNotes },
}
