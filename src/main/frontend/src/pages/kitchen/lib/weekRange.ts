/** Monday of the ISO week containing `date`, at local midnight. */
export function startOfWeek(date: Date): Date {
  const day = date.getDay() // 0 = Sunday .. 6 = Saturday
  const diffToMonday = (day === 0 ? -6 : 1) - day
  const monday = new Date(date)
  monday.setDate(date.getDate() + diffToMonday)
  monday.setHours(0, 0, 0, 0)
  return monday
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

export function toISODate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** The seven consecutive days starting at `monday`. */
/** A week, as the seven days it is — so the first and the last need no checking. */
export type WeekDates = readonly [Date, Date, Date, Date, Date, Date, Date]

export function weekDates(monday: Date): WeekDates {
  // Written out rather than generated: Array.from cannot produce a tuple, and the length is the
  // whole point — callers read days[0] and days[6] to bound the query.
  return [
    addDays(monday, 0), addDays(monday, 1), addDays(monday, 2), addDays(monday, 3),
    addDays(monday, 4), addDays(monday, 5), addDays(monday, 6),
  ]
}
