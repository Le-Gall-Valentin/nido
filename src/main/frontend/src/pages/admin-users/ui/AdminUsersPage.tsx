import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Alert, Button, Pagination, SearchInput, CTA_BUTTON_STYLE } from '@/shared/ui'
import { useDebouncedValue, pageAfterRemoval } from '@/shared/lib'
import { useAuth } from '@/features/auth'
import type { AdminUser } from '@/entities/user'
import { adminUsersApi } from '../api/adminUsersApi'
import type { IAdminUsersApi } from '../model/IAdminUsersApi'
import { AdminUsersApiProvider } from '../model/adminUsersApiContext'
import { useUsers } from '../model/useUsers'
import {
  useCreateUser,
  useUpdateUserRole,
  useDeleteUser,
  useResetTotp,
  useToggleUserActive,
} from '../model/useUserMutations'
import { UsersTable } from './UsersTable'
import { UsersCardList } from './UsersCardList'
import { CreateUserModal } from './CreateUserModal'
import { EditUserRoleModal } from './EditUserRoleModal'
import { DeleteUserModal } from './DeleteUserModal'
import { ResetTotpModal } from './ResetTotpModal'
import { ProtectionRulesPanel } from './ProtectionRulesPanel'

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
  const [searchInput, setSearchInput] = useState('')
  const search = useDebouncedValue(searchInput, 300)
  const { data, isPending, isError: loadError, isPlaceholderData } = useUsers(page, search)

  const [toggleError, setToggleError] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<AdminUser | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AdminUser | null>(null)
  const [resetTotpTarget, setResetTotpTarget] = useState<AdminUser | null>(null)

  const createUser = useCreateUser()
  const updateUserRole = useUpdateUserRole()
  const deleteUser = useDeleteUser()
  const resetTotp = useResetTotp()
  const toggleActive = useToggleUserActive(page, search)

  function handleSearchChange(value: string) {
    setSearchInput(value)
    setPage(0)
  }

  function handleToggle(user: AdminUser) {
    setToggleError(false)
    toggleActive.mutate(user, { onError: () => setToggleError(true) })
  }

  function handleDeleteSuccess() {
    setPage(pageAfterRemoval(page, data?.content.length ?? 0, isPlaceholderData))
    setDeleteTarget(null)
  }

  if (!currentUser) return null

  const users = data?.content ?? []
  const pendingToggleId = toggleActive.isPending ? toggleActive.variables?.id : null
  const totalElements = data?.totalElements ?? 0
  const pageSize = data?.size ?? 20
  const totalPages = totalElements > 0 ? Math.ceil(totalElements / pageSize) : 1
  const showPagination = !isPending && totalPages > 1

  return (
    <div className="py-5 px-6 mx-auto">
      {/* Header */}
      <div className="flex flex-col gap-3 mb-4 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-sm text-fg-2 mt-1">{t('subtitle')}</p>
        </div>
        <Button
          onClick={() => setCreateOpen(true)}
          className="self-start shrink-0 border-transparent font-semibold"
          style={CTA_BUTTON_STYLE}
        >
          <Plus className="size-4" />
          {t('action.create')}
        </Button>
      </div>

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

      <SearchInput
        value={searchInput}
        onChange={handleSearchChange}
        placeholder={t('search.placeholder')}
        clearLabel={t('search.clear')}
        className="mb-3 max-w-md"
      />

      {/* Desktop: table. Mobile: stacked cards (no horizontal scroll). */}
      <div className="hidden md:block">
        <UsersTable
          users={users}
          isLoading={isPending}
          currentUser={currentUser}
          pendingToggleId={pendingToggleId}
          onToggleActive={handleToggle}
          onEditRole={setEditTarget}
          onResetTotp={setResetTotpTarget}
          onDelete={setDeleteTarget}
        />
      </div>
      <div className="md:hidden">
        <UsersCardList
          users={users}
          isLoading={isPending}
          currentUser={currentUser}
          pendingToggleId={pendingToggleId}
          onToggleActive={handleToggle}
          onEditRole={setEditTarget}
          onResetTotp={setResetTotpTarget}
          onDelete={setDeleteTarget}
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

      <ProtectionRulesPanel />

      {/* Modals */}
      {createOpen && (
        <CreateUserModal
          caller={currentUser}
          onClose={() => setCreateOpen(false)}
          onCreate={(u, e, p, r) => createUser.mutateAsync({ username: u, email: e, password: p, role: r })}
          onSuccess={() => { setPage(0); setCreateOpen(false) }}
        />
      )}

      {editTarget && (
        <EditUserRoleModal
          target={editTarget}
          caller={currentUser}
          onClose={() => setEditTarget(null)}
          onUpdate={(id, role) => updateUserRole.mutateAsync({ id, role })}
          onSuccess={() => setEditTarget(null)}
        />
      )}

      {deleteTarget && (
        <DeleteUserModal
          user={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDelete={deleteUser.mutateAsync}
          onSuccess={handleDeleteSuccess}
        />
      )}

      {resetTotpTarget && (
        <ResetTotpModal
          user={resetTotpTarget}
          onClose={() => setResetTotpTarget(null)}
          onReset={resetTotp.mutateAsync}
          onSuccess={() => setResetTotpTarget(null)}
        />
      )}
    </div>
  )
}
