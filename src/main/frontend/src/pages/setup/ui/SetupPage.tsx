import type { SetupStatus } from '../model/types'

/** Placeholder until the wizard lands. */
export function SetupPage({ status }: { status: SetupStatus }) {
  return status.required ? null : null
}
