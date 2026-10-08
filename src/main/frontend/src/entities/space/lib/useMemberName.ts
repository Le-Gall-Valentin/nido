import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '../model/types'

/**
 * How every screen names a space member: by account name, or by what became of them. Never by their
 * id, which is all a task or a payment still holds once its author has left or deleted their account.
 * Pass undefined while the list is not known: nobody can then be said to have left.
 */
export function useMemberName(members: SpaceMember[] | undefined): (userId: string) => string {
  const { t } = useTranslation('common')
  return useCallback((userId: string) => {
    if (!members) return t('member.generic')
    const member = members.find((m) => m.userId === userId)
    if (!member) return t('member.former')
    return member.username ?? t('member.deleted')
  }, [members, t])
}
