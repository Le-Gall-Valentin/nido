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
export function weekDates(monday: Date): Date[] {
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i))
}
