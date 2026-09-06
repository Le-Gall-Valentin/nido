import type { ContributionInput, RecurrenceInput, RecurringSeries, TransactionType } from './types'

/** Port for recurring transaction series CRUD. */
export interface IRecurringSeriesApi {
  listRecurringSeries(spaceId: string): Promise<RecurringSeries[]>
  createRecurringSeries(
    spaceId: string, label: string, amount: number, type: TransactionType, categoryId: string, payerId: string | null,
    contributors: ContributionInput[], recurrence: RecurrenceInput
  ): Promise<RecurringSeries>
  updateRecurringSeries(
    spaceId: string, seriesId: string, label: string, amount: number, type: TransactionType, categoryId: string,
    payerId: string | null, contributors: ContributionInput[], recurrence: RecurrenceInput
  ): Promise<RecurringSeries>
  deleteRecurringSeries(spaceId: string, seriesId: string): Promise<void>
}
