import type { LucideIcon } from 'lucide-react'
import { Calendar, CheckSquare, ClipboardList, CookingPot, LayoutDashboard, Lock, Server, Settings, Shield, ShoppingCart, SlidersHorizontal, User, Users, Wallet, UserCircle } from 'lucide-react'
import { isAdminRole, type UserRole } from '@/entities/user'
import { ROUTES } from '@/shared/config'

export interface NavItemConfig {
  id: string
  /**
   * Resolves the item's destination given the caller's current space
   * (see useCurrentSpaceId). Items that don't need a space (Groupes,
   * Compte, Administration) ignore the argument; items that do (Cuisine)
   * return undefined while it isn't resolved yet, so the item is skipped
   * rather than linking nowhere.
   */
  to: (spaceId: string | undefined) => string | undefined
  icon: LucideIcon
  labelKey: string
  adminOnly?: boolean
  /** The instance settings concern the whole installation: the super admin alone. */
  superAdminOnly?: boolean
  children?: NavItemConfig[]
}

// The sidebar shows this exact list at all times, for every authenticated
// route — it never depends on whether the current URL happens to carry a
// spaceId. Only modules that actually have a route/page belong here — no
// placeholder entries for future modules (Documents).
//
// Order: the dashboard first — it is where the app lands, and the user asked
// for it to sit above everything — then Finances (the user's explicit
// priority among the modules: money is the module they want front and
// center), then Organisation, then Cuisine, then Membres & groupes, then
// Administration, then Paramètres. The mockup has exactly one "Membres &
// groupes" entry, no separate per-space "Membres" item: /spaces already lets
// you drill into a group to reach its members page (SpaceListSection →
// SpaceMembersPage), the same "click a group card to open its detail" flow
// the mockup uses.
export const NAV_CONFIG: NavItemConfig[] = [
  { id: 'nav:dashboard', to: (spaceId) => (spaceId ? ROUTES.spaceDashboard(spaceId) : undefined), icon: LayoutDashboard, labelKey: 'nav.dashboard' },
  { id: 'nav:finance', to: (spaceId) => (spaceId ? ROUTES.spaceFinance(spaceId) : undefined), icon: Wallet, labelKey: 'nav.finance' },
  {
    id: 'nav:organisation',
    to: (spaceId) => (spaceId ? ROUTES.spaceOrganisationCourses(spaceId) : undefined),
    icon: ClipboardList,
    labelKey: 'nav.organisation',
    children: [
      { id: 'nav:organisation:courses', to: (spaceId) => (spaceId ? ROUTES.spaceOrganisationCourses(spaceId) : undefined), icon: ShoppingCart, labelKey: 'nav.organisation_courses' },
      { id: 'nav:organisation:tasks', to: (spaceId) => (spaceId ? ROUTES.spaceOrganisationTasks(spaceId) : undefined), icon: CheckSquare, labelKey: 'nav.organisation_tasks' },
      { id: 'nav:organisation:calendar', to: (spaceId) => (spaceId ? ROUTES.spaceOrganisationCalendar(spaceId) : undefined), icon: Calendar, labelKey: 'nav.organisation_calendar' },
    ],
  },
  {
    id: 'nav:kitchen',
    to: (spaceId) => (spaceId ? ROUTES.spaceKitchenRecipes(spaceId) : undefined),
    icon: CookingPot,
    labelKey: 'nav.kitchen',
    children: [
      { id: 'nav:kitchen:recipes', to: (spaceId) => (spaceId ? ROUTES.spaceKitchenRecipes(spaceId) : undefined), icon: CookingPot, labelKey: 'nav.kitchen_recipes' },
      { id: 'nav:kitchen:menu', to: (spaceId) => (spaceId ? ROUTES.spaceKitchenMenu(spaceId) : undefined), icon: Calendar, labelKey: 'nav.kitchen_menu' },
    ],
  },
  { id: 'nav:spaces', to: () => ROUTES.SPACES, icon: Users, labelKey: 'nav.groups' },
  { id: 'nav:users', adminOnly: true, to: () => ROUTES.ADMIN_USERS, icon: Shield, labelKey: 'nav.administration' },
  { id: 'nav:instance-settings', superAdminOnly: true, to: () => ROUTES.ADMIN_SETTINGS, icon: Server, labelKey: 'nav.instance_settings' },
  {
    // The mockup's fourth sub-category, Notifications, has no backing feature
    // (no notification system exists yet) — only the three that map to real
    // account content are listed here.
    id: 'nav:account', to: () => ROUTES.ACCOUNT_PROFILE, icon: Settings, labelKey: 'nav.settings',
    children: [
      { id: 'nav:account:profile', to: () => ROUTES.ACCOUNT_PROFILE, icon: User, labelKey: 'nav.settings_profile' },
      { id: 'nav:account:security', to: () => ROUTES.ACCOUNT_SECURITY, icon: Lock, labelKey: 'nav.settings_security' },
      { id: 'nav:account:preferences', to: () => ROUTES.ACCOUNT_PREFERENCES, icon: SlidersHorizontal, labelKey: 'nav.settings_preferences' },
      { id: 'nav:account:personal-space', to: () => ROUTES.ACCOUNT_PERSONAL_SPACE, icon: UserCircle, labelKey: 'nav.settings_personal_space' },
    ],
  },
]

/** Whether the item shows for this role: the administration for admins, the instance settings for the super admin alone. */
export function isNavItemVisible(item: NavItemConfig, role: UserRole | undefined): boolean {
  if (item.superAdminOnly) return role === 'SUPER_ADMIN'
  if (item.adminOnly) return isAdminRole(role)
  return true
}
