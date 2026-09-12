import { client } from '@/shared/api'

/**
 * The one write this page needs. Kept here rather than reaching for the spaces page's own port: that
 * port is shaped around managing shared spaces, and account settings has no business carrying its
 * provider just to change one field.
 */
export const personalSpaceApi = {
  async updateTimezone(spaceId: string, timezone: string): Promise<void> {
    await client.patch(`/spaces/${spaceId}`, { timezone })
  },
}
