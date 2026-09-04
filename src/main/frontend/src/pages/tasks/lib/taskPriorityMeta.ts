import type { TaskPriority } from '@/entities/tasks'

interface PriorityMeta {
  labelKey: string
  dotClassName: string
  textClassName: string
}

export const TASK_PRIORITY_ORDER: TaskPriority[] = ['HIGH', 'MED', 'LOW']

export const TASK_PRIORITY_META: Record<TaskPriority, PriorityMeta> = {
  HIGH: { labelKey: 'priority.HIGH', dotClassName: 'bg-status-red', textClassName: 'text-status-red' },
  MED: { labelKey: 'priority.MED', dotClassName: 'bg-status-orange', textClassName: 'text-status-orange' },
  LOW: { labelKey: 'priority.LOW', dotClassName: 'bg-status-green', textClassName: 'text-status-green' },
}
