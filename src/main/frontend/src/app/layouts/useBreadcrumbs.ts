import { useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ROUTES, ROUTE_SEGMENTS } from '@/shared/config'

export interface Breadcrumb {
  label: string
  to?: string
}

export function useBreadcrumbs(): Breadcrumb[] {
  const { pathname } = useLocation()
  const { t } = useTranslation('shell')

  return useMemo(() => {
    const segments = pathname.split('/').filter(Boolean)
    // segments[0] = ADMINISTRATION | ACCOUNT
    // segments[1] = USERS | …

    const adminRoot: Breadcrumb = { label: t('breadcrumb.administration'), to: ROUTES.ADMINISTRATION }

    if (segments[0] === ROUTE_SEGMENTS.ADMINISTRATION) {
      const labelMap: Record<string, string> = {
        [ROUTE_SEGMENTS.USERS]: t('breadcrumb.admin_users'),
      }
      if (!segments[1]) return [adminRoot]
      return [adminRoot, { label: labelMap[segments[1]] ?? segments[1] }]
    }

    if (segments[0] === ROUTE_SEGMENTS.ACCOUNT) {
      return [{ label: t('breadcrumb.account') }]
    }

    return []
  }, [pathname, t])
}