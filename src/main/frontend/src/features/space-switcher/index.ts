import './locales'

export type { ISpacesApi } from './model/ISpacesApi'
export { spacesApi, SpaceNotAccessibleError } from './api/spacesApi'
export { SpacesApiProvider, useSpacesApi } from './model/spacesApiContext'
export { useMySpaces, SPACES_QUERY_KEY } from './model/useMySpaces'
export { activeSpaceStore, LAST_SPACE_STORAGE_KEY } from './model/activeSpaceStore'
export { useActiveSpace } from './model/useActiveSpace'
export { SpaceSwitcher } from './ui/SpaceSwitcher'
