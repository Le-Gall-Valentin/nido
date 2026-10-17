import type {
  CreatedSubscriptionLink, ExportableSource, ExternalFeed, ExternalFeedInput, ImportReport, SubscriptionLink, SubscriptionLinkInput,
} from './types'

/** Port for what goes in and out of the calendar: an export, addresses to subscribe to, an import, followed calendars. */
export interface ICalendarSyncApi {
  exportCalendar(spaceId: string, sources: ExportableSource[], onlyMine: boolean): Promise<{ blob: Blob; filename: string }>
  listSubscriptionLinks(spaceId: string): Promise<SubscriptionLink[]>
  createSubscriptionLink(spaceId: string, input: SubscriptionLinkInput): Promise<CreatedSubscriptionLink>
  revokeSubscriptionLink(spaceId: string, linkId: string): Promise<void>
  analyseImport(spaceId: string, file: Blob): Promise<ImportReport>
  importFile(spaceId: string, file: Blob): Promise<ImportReport>
  listExternalFeeds(spaceId: string): Promise<ExternalFeed[]>
  followExternalFeed(spaceId: string, input: ExternalFeedInput): Promise<ExternalFeed>
  updateExternalFeed(spaceId: string, feedId: string, input: { name: string; color: string | null }): Promise<ExternalFeed>
  removeExternalFeed(spaceId: string, feedId: string): Promise<void>
  refreshExternalFeed(spaceId: string, feedId: string): Promise<ExternalFeed>
}
