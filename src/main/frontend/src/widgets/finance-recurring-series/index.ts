import './locales'

export { FinanceRecurringSeriesPanel } from './ui/FinanceRecurringSeriesPanel'
// ContributorsPicker and two contribution helpers are exported even though this widget
// is named after recurring series: pages/finance needs them for TransactionFormModal,
// AddContributionModal and TransactionDetailModal, and there is nowhere else they can live
// without being duplicated — a widget may not import another widget, and shared/ may not
// import entities, which these do.
export { ContributorsPicker } from './ui/ContributorsPicker'
export { resolveContributorsOrError } from './lib/resolveContributorsOrError'
export { contributionLabelKeys } from './lib/contributionLabelKeys'
export type { RecurringSeriesFormInput } from './ui/RecurringSeriesFormModal'
