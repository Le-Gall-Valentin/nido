import type { PermissionResult } from '../lib/permissions'

/**
 * Tooltip text for a permission-gated control: the fallback label when the
 * action is allowed, the localized denial reason when it is not.
 */
export function permissionDenialTitle(
  check: PermissionResult,
  translate: (key: string) => string,
  fallback: string,
): string {
  return check.ok ? fallback : translate(`perm.${check.reason}`)
}
