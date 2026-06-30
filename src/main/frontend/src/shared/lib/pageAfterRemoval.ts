/**
 * Page to land on after removing one row from a paginated list: step back when
 * the removed row was the last one on a page beyond the first. Pass committed
 * (non-placeholder) data so a mid-transition snapshot can't drive the decision.
 */
export function pageAfterRemoval(page: number, visibleCount: number, isPlaceholderData: boolean): number {
  if (!isPlaceholderData && visibleCount === 1 && page > 0) return page - 1
  return page
}
