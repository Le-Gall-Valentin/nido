import { registerLocales } from '@/shared/lib/registerLocales'
import en from './en.json'
import fr from './fr.json'

// Registers into the SAME `finance` namespace the page uses — see the equivalent note in
// widgets/task-management/locales/index.ts. The whole `savings` group is duplicated rather
// than sliced at its second level: the form only reads a dozen of its keys, but a nested
// split is fragile to maintain and the consistency test already makes duplication safe.
registerLocales('finance', { en, fr })
