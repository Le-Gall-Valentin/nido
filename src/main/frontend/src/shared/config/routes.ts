export const ROUTES = {
  LOGIN: '/login',

  // Section redirect (auto-redirect to first item)
  ADMINISTRATION: '/administration',

  // Administration
  ADMIN_USERS: '/administration/users',

  // Account
  ACCOUNT: '/account',
  ACCOUNT_PROFILE: '/account/profile',
  ACCOUNT_SECURITY: '/account/security',
  ACCOUNT_PREFERENCES: '/account/preferences',

  // Spaces (contexts)
  SPACES: '/spaces',
  space: (spaceId: string) => `/s/${spaceId}`,
  spaceMembers: (spaceId: string) => `/s/${spaceId}/members`,
  spaceKitchenRecipes: (spaceId: string) => `/s/${spaceId}/kitchen/recipes`,
  spaceKitchenRecipe: (spaceId: string, recipeId: string) => `/s/${spaceId}/kitchen/recipes/${recipeId}`,
  spaceKitchenMenu: (spaceId: string) => `/s/${spaceId}/kitchen/menu`,
  spaceOrganisationCourses: (spaceId: string) => `/s/${spaceId}/organisation/courses`,
} as const

export const ROUTE_SEGMENTS = {
  ADMINISTRATION: 'administration',
  ACCOUNT: 'account',
  USERS: 'users',
} as const