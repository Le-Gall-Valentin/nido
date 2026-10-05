import type { ReactNode } from 'react'
import type { SettingsGroup } from '@/entities/instance-settings'
import type { SettingsGroupState } from '../../model/useSettingsGroup'

export interface GroupExtrasProps {
  group: SettingsGroup
  state: SettingsGroupState
}

/** What one block adds to the common card: notes under its fields, actions beside its Save button. */
export interface GroupExtras {
  Notes?: (props: GroupExtrasProps) => ReactNode
  Actions?: (props: GroupExtrasProps) => ReactNode
}
