import type { ICategoriesApi } from './ICategoriesApi'
import type { IBudgetsApi } from './IBudgetsApi'
import type { ITransactionsApi } from './ITransactionsApi'
import type { IRecurringSeriesApi } from './IRecurringSeriesApi'
import type { IFinanceStatsApi } from './IFinanceStatsApi'
import type { IBalancesApi } from './IBalancesApi'
import type { ISavingsGoalsApi } from './ISavingsGoalsApi'

/**
 * Port for the Finance page, composed from the per-concern sub-interfaces below.
 * Consumers (hooks) should depend on the narrowest sub-interface that covers what
 * they call, never on this composed type or the concrete axios-backed implementation,
 * which is injected as a whole through FinanceApiProvider.
 */
export interface IFinanceApi extends
  ICategoriesApi, IBudgetsApi, ITransactionsApi, IRecurringSeriesApi, IFinanceStatsApi, IBalancesApi, ISavingsGoalsApi {}
