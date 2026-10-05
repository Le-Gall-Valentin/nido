import { lazy } from 'react'
export { setupApi } from './api/setupApi'
export { useSetupStatus, SETUP_STATUS_KEY, type SetupStatusState } from './model/useSetupStatus'
export type { ISetupApi } from './model/ISetupApi'
export type { SetupStatus } from './model/types'
export { SetupCodeInvalidError, SetupAlreadyDoneError, KeyNotSavedError, AdminRefusedError } from './model/errors'
/** Loaded on demand: most visits never see the setup, and its code and words need not come along. */
export const SetupPage = lazy(() => import('./ui/SetupPage').then((module) => ({ default: module.SetupPage })))
