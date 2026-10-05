import { useSettingWording } from '@/entities/instance-settings'
import type { GroupExtrasProps } from './types'

/** Mail needs the public address for its links: the refusal is said here, where mail was being saved. */
export function MailNotes({ state }: GroupExtrasProps) {
  const wording = useSettingWording()
  const problem = state.problems['public-url']
  return problem ? <p className="mt-3 text-[12.5px] text-status-red">{wording.problem(problem)}</p> : null
}
