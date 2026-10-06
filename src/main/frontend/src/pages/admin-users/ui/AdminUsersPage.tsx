import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Alert, Button, DebouncedSearchInput, Pagination, CTA_BUTTON_STYLE } from '@/shared/ui'
import { pageAfterRemoval } from '@/shared/lib'
import { useAuth } from '@/features/auth'
import type { AdminUser, IAdminUsersApi } from '@/entities/user'
import { adminUsersApi, AdminUsersApiProvider, useToggleUserActive, useUsers } from '@/entities/user'
import { useMailAvailability } from '@/entities/capabilities'
import { UsersTable } from './UsersTable'
import { UsersCardList } from './UsersCardList'
import { UserDialogs, type UserDialog } from './UserDialogs'
import { ProtectionRulesPanel } from './ProtectionRulesPanel'
import type { UserRowCallbacks } from './userRowCallbacks'

interface AdminUsersPageProps {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: IAdminUsersApi
}

/**
 * Slice composition root: provisions the admin users API at its own boundary,
 * so the app/router never has to know about this dependency.
 */
export function AdminUsersPage({ api = adminUsersApi }: AdminUsersPageProps = {}) {
  return (
    <AdminUsersApiProvider api={api}>
      <AdminUsersPageContent />
    </AdminUsersApiProvider>
  )
}

function AdminUsersPageContent() {
  const { t } = useTranslation('adminUsers')
  const currentUser = useAuth(s => s.user)

  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const { data, isPending, isError: loadError, isPlaceholderData } = useUsers(page, search)

  const [toggleError, setToggleError] = useState(false)
  const [dialog, setDialog] = useState<UserDialog>(null)
  const closeDialog = () => setDialog(null)

  const toggleActive = useToggleUserActive(page, search)
  const mail = useMailAvailability()

  // Settled value only: the field below keeps the keystrokes, so this runs once the typing stops.
  const handleSearch = useCallback((value: string) => {
    setSearch(value)
    setPage(0)
  }, [])

  function handleToggle(user: AdminUser) {
    setToggleError(false)
    // Deactivating cuts someone off and tells them by mail: confirmed first. Reactivating is harmless.
    if (user.isActive) {
      setDialog({ kind: 'deactivate', user })
      return
    }
    toggleActive.mutate(user, { onError: () => setToggleError(true) })
  }

  function handleDeleted() {
    setPage(pageAfterRemoval(page, data?.content.length ?? 0, isPlaceholderData))
    closeDialog()
  }

  const rowCallbacks: UserRowCallbacks = {
    onToggleActive: handleToggle,
    onEditRole: (user) => setDialog({ kind: 'edit_role', user }),
    onResetTotp: (user) => setDialog({ kind: 'reset_totp', user }),
    onDelete: (user) => setDialog({ kind: 'delete', user }),
    onResendInvitation: (user) => setDialog({ kind: 'resend', user }),
  }

  if (!currentUser) return null

  const users = data?.content ?? []
  const pendingToggleId = toggleActive.isPending ? toggleActive.variables?.id : null
  const totalElements = data?.totalElements ?? 0
  const pageSize = data?.size ?? 20
  const totalPages = totalElements > 0 ? Math.ceil(totalElements / pageSize) : 1
  const showPagination = !isPending && totalPages > 1
  // Each row names its invitation action after how a new link would leave: listed once that is known.
  const isLoading = isPending || mail === 'loading'

  return (
    <div className="mx-auto max-w-[1180px] px-5 py-6 md:px-10 md:py-[34px]">
      {/* Header */}
      <div className="flex flex-col gap-3 mb-[22px] sm:flex-row sm:items-end sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-[15px] text-fg-2 mt-1">
            {t(currentUser.role === 'SUPER_ADMIN' ? 'subtitle_super' : 'subtitle_admin')}
          </p>
        </div>
        <Button
          onClick={() => setDialog({ kind: 'create' })}
          className="self-start shrink-0 border-transparent font-semibold"
          style={{ ...CTA_BUTTON_STYLE, boxShadow: 'var(--btn-primary-shadow)' }}
        >
          <Plus className="size-4" />
          {t('action.create')}
        </Button>
      </div>

      <ProtectionRulesPanel />

      {loadError && (
        <Alert variant="error" className="mb-4">{t('load_error')}</Alert>
      )}

      {toggleError && (
        <Alert
          variant="error"
          className="mb-4"
          onDismiss={() => setToggleError(false)}
          dismissLabel={t('close')}
        >
          {t('mutation_error')}
        </Alert>
      )}

      <DebouncedSearchInput
        onSearch={handleSearch}
        placeholder={t('search.placeholder')}
        clearLabel={t('search.clear')}
        className="mb-3 max-w-md"
      />

      {/* Desktop: table. Mobile: stacked cards (no horizontal scroll). */}
      <div className="hidden md:block">
        <UsersTable
          users={users}
          isLoading={isLoading}
          currentUser={currentUser}
          mail={mail}
          pendingToggleId={pendingToggleId}
          {...rowCallbacks}
        />
      </div>
      <div className="md:hidden">
        <UsersCardList
          users={users}
          isLoading={isLoading}
          currentUser={currentUser}
          mail={mail}
          pendingToggleId={pendingToggleId}
          {...rowCallbacks}
        />
      </div>

      {showPagination && (
        <div className="mt-3">
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
            pageLabel={t('pagination.page', { current: page + 1, total: totalPages })}
            prevLabel={t('pagination.prev')}
            nextLabel={t('pagination.next')}
            summary={t('pagination.total', { count: totalElements })}
            isTransitioning={isPlaceholderData}
          />
        </div>
      )}

      <UserDialogs
        dialog={dialog}
        caller={currentUser}
        mail={mail}
        onClose={closeDialog}
        onCreated={() => { setPage(0); closeDialog() }}
        onDeleted={handleDeleted}
        onDeactivate={(user) => toggleActive.mutateAsync(user)}
      />
    </div>
  )
}
