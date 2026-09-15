import { registerLocales } from '@/shared/lib/registerLocales'
import en from './en.json'
import fr from './fr.json'

// Registers into the SAME `finance` namespace the page uses — see the equivalent note in
// widgets/task-management/locales/index.ts. Every group here is also used by pages/finance,
// so this bundle is a deliberate duplicate rather than a slice: it is what lets the calendar
// mount this widget without loading the finance page at all.
registerLocales('finance', { en, fr })
