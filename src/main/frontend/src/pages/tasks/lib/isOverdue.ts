/**
 * Whether a task's due date has passed, on a given calendar.
 *
 * `today` is a parameter rather than read here, because whose "today" it is turns out to be the
 * whole question: a task belongs to a space, a space keeps its own timezone, and a member reading
 * from another continent must see the same lateness as everyone else in the household. Computing it
 * from the viewer's clock struck tasks through while their day was still going.
 *
 * Both arguments are civil dates as `YYYY-MM-DD`, which compare correctly as strings.
 */
export function isOverdue(dueDate: string | null, today: string): boolean {
  if (!dueDate) return false
  return dueDate < today
}
