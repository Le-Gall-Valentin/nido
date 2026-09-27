/**
 * GET /api/spaces/{spaceId}/dashboard, mirrored by hand like every other entity here. Field names are
 * the backend's, verbatim — see "REST API" in docs/superpowers/specs/2026-09-26-dashboard-design.md.
 *
 * The server has already decided everything: which blocks are relevant, what goes in them, in which
 * order. A card key that is missing or null means "nothing to show" and the page hides that block;
 * `{ status: 'UNAVAILABLE' }` means its source failed and the page says so in its place.
 */
export type DashboardSpaceType = 'PERSONAL' | 'SHARED'
export type DashboardRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
export type Severity = 'HIGH' | 'MEDIUM' | 'INFO'
export type MealCategory = 'PLAT' | 'EXPRESS' | 'VEGETARIAN' | 'DESSERT' | 'SOUP'

export type AttentionItem =
  | { kind: 'OVERDUE_TASKS'; severity: 'HIGH'; count: number; titles: string[] }
  | { kind: 'BUDGET_OVERRUN'; severity: 'HIGH'; categoryId: string; label: string; spent: number; limit: number }
  | { kind: 'DEBT'; severity: 'MEDIUM'; toMemberId: string; amount: number }
  | {
      kind: 'INVITATION'; severity: 'INFO'; invitationId: string; spaceName: string; spaceGlyph: string
      spaceAccent: string; role: DashboardRole; invitedByUsername: string | null; expiresAt: string
    }

export interface AgendaEvent {
  id: string
  title: string
  location: string | null
  /** An event colour token (`event-violet`…), or null for the default. */
  color: string | null
  startDate: string
  endDate: string
  /** `HH:mm` or `HH:mm:ss`, as the calendar sends it; null for an all-day event. */
  startTime: string | null
  endTime: string | null
  participantIds: string[]
}

export interface TaskItem {
  id: string
  title: string
  dueDate: string | null
  priority: 'HIGH' | 'MED' | 'LOW'
  status: 'TODO' | 'DOING' | 'DONE'
  assigneeIds: string[]
  subtasksDone: number
  subtasksTotal: number
  recurring: boolean
  /**
   * Whether it calls for the caller's action — assigned to them or to nobody, or any task of their
   * personal space. The server's rule, decided once; the page never works it out again.
   */
  mine: boolean
}

export interface AgendaCard {
  allDay: AgendaEvent[]
  timed: AgendaEvent[]
  dueToday: TaskItem[]
  tomorrow: AgendaEvent | null
}

export interface MealItem {
  entryId: string
  /** Null, with name, category and minutes, when the recipe was deleted after being planned. */
  recipeId: string | null
  recipeName: string | null
  category: MealCategory | null
  minutes: number | null
  portions: number
}

export interface MenuCard {
  today: MealItem[]
  tomorrow: MealItem[]
  /** The dates in [today, today+6] with nothing planned, ascending. */
  unplannedDays: string[]
}

export interface TasksCard {
  overdue: TaskItem[]
  thisWeek: TaskItem[]
  inProgress: TaskItem[]
  openCount: number
  openCountMine: number
}

export interface BudgetWatch {
  categoryId: string
  label: string
  color: string | null
  spent: number
  limit: number
  status: 'WARNING' | 'OVER'
}

export interface UpcomingOperation {
  date: string
  label: string
  amount: number
  type: 'EXPENSE' | 'INCOME'
  seriesId: string
}

export interface MemberBalance {
  memberId: string
  amount: number
  direction: 'I_OWE' | 'OWES_ME'
}

export interface FinanceCard {
  /** `yyyy-MM` */
  month: string
  balance: number
  totalExpense: number
  totalIncome: number
  remainingBudget: number
  budgetsToWatch: BudgetWatch[]
  upcoming: UpcomingOperation[]
  /** Null in a personal space. */
  balances: MemberBalance[] | null
}

export interface SavingsGoalItem {
  goalId: string
  name: string
  glyph: string
  color: string
  target: number
  contributed: number
  targetDate: string | null
  state: 'REACHED' | 'PAST_DUE' | 'DUE_SOON' | 'IN_PROGRESS'
  monthlyNeeded: number | null
}

export interface SavingsCard {
  goals: SavingsGoalItem[]
}

export interface ShoppingGroup {
  categoryId: string
  name: string
  count: number
  preview: string[]
}

export interface ShoppingCard {
  remaining: number
  categories: ShoppingGroup[]
}

export type CardResult<T> = { status: 'OK'; data: T } | { status: 'UNAVAILABLE' }

export interface DashboardCards {
  agenda?: CardResult<AgendaCard> | null
  menu?: CardResult<MenuCard> | null
  tasks?: CardResult<TasksCard> | null
  finance?: CardResult<FinanceCard> | null
  savings?: CardResult<SavingsCard> | null
  shopping?: CardResult<ShoppingCard> | null
}

export interface Dashboard {
  date: string
  spaceType: DashboardSpaceType
  canWrite: boolean
  /** False when a source failed — even the card-less invitations: an empty `attention` then does not mean "all clear". */
  complete: boolean
  attention: AttentionItem[]
  cards: DashboardCards
}
