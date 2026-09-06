import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ConfirmDeleteModal } from './ConfirmDeleteModal'

describe('ConfirmDeleteModal', () => {
  it('confirms deletion when the confirm button is clicked', () => {
    const onConfirm = vi.fn()
    render(
      <ConfirmDeleteModal
        title='Delete "Courses"?' message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} error={null}
      />
    )

    expect(screen.getByRole('heading', { level: 3, name: /Courses/ })).toBeDefined()
    fireEvent.click(screen.getByText('Delete'))

    expect(onConfirm).toHaveBeenCalled()
  })

  it('cancels without confirming when the cancel button is clicked', () => {
    const onCancel = vi.fn()
    render(
      <ConfirmDeleteModal
        title="Delete?" message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={vi.fn()} onCancel={onCancel} isPending={false} error={null}
      />
    )

    fireEvent.click(screen.getByText('Cancel'))

    expect(onCancel).toHaveBeenCalled()
  })

  it('shows the error message when provided', () => {
    render(
      <ConfirmDeleteModal
        title="Delete?" message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} error="Something went wrong."
      />
    )

    expect(screen.getByText('Something went wrong.')).toBeDefined()
  })

  it('shows no error message when none is provided', () => {
    render(
      <ConfirmDeleteModal
        title="Delete?" message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} error={null}
      />
    )

    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('renders extra content passed as children between the message and the buttons', () => {
    render(
      <ConfirmDeleteModal
        title="Delete?" message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} error={null}
      >
        <span>extra-identity-strip</span>
      </ConfirmDeleteModal>
    )

    expect(screen.getByText('extra-identity-strip')).toBeDefined()
  })

  it('disables the cancel button and shows a loading state on the confirm button while pending', () => {
    render(
      <ConfirmDeleteModal
        title="Delete?" message="This action is permanent." confirmLabel="Delete" cancelLabel="Cancel"
        onConfirm={vi.fn()} onCancel={vi.fn()} isPending error={null}
      />
    )

    expect(screen.getByText('Cancel')).toHaveProperty('disabled', true)
  })
})
