import { isAxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError, ForbiddenError, NotFoundError } from '@/shared/lib'
import type { ITasksApi } from '../model/ITasksApi'
import type { Task } from '../model/types'

function handleError(error: unknown): never {
  if (isAxiosError(error)) {
    const status = error.response?.status
    if (status === 429) throw new RateLimitError()
    if (status === 403) throw new ForbiddenError()
    if (status === 404) throw new NotFoundError()
    if (status !== undefined) throw new ServerError()
  }
  throw new NetworkError()
}

export const tasksApi: ITasksApi = {
  async listTasks(spaceId) {
    try {
      const res = await client.get<Task[]>(`/spaces/${spaceId}/tasks`)
      return res.data
    } catch (error) { handleError(error) }
  },

  async createTask(spaceId, title, priority, dueDate, assigneeIds, subtasks) {
    try {
      const res = await client.post<Task>(`/spaces/${spaceId}/tasks`, { title, priority, dueDate, assigneeIds, subtasks })
      return res.data
    } catch (error) { handleError(error) }
  },

  async createRecurringTask(spaceId, title, priority, subtasks, recurrence) {
    try {
      const res = await client.post<Task>(`/spaces/${spaceId}/tasks`, { title, priority, subtasks, recurrence })
      return res.data
    } catch (error) { handleError(error) }
  },

  async updateTask(spaceId, taskId, title, priority, dueDate, assigneeIds) {
    try {
      const res = await client.patch<Task>(`/spaces/${spaceId}/tasks/${taskId}`, { title, priority, dueDate, assigneeIds })
      return res.data
    } catch (error) { handleError(error) }
  },

  async changeTaskStatus(spaceId, taskId, status) {
    try {
      const res = await client.post<Task>(`/spaces/${spaceId}/tasks/${taskId}/status`, { status })
      return res.data
    } catch (error) { handleError(error) }
  },

  async toggleSubtask(spaceId, taskId, subtaskId) {
    try {
      await client.post(`/spaces/${spaceId}/tasks/${taskId}/subtasks/${subtaskId}/toggle`)
    } catch (error) { handleError(error) }
  },

  async deleteTask(spaceId, taskId) {
    try {
      await client.delete(`/spaces/${spaceId}/tasks/${taskId}`)
    } catch (error) { handleError(error) }
  },

  async moveTask(spaceId, taskId, destinationSpaceId) {
    try {
      const res = await client.post<Task>(`/spaces/${spaceId}/tasks/${taskId}/move`, { destinationSpaceId })
      return res.data
    } catch (error) { handleError(error) }
  },
}
