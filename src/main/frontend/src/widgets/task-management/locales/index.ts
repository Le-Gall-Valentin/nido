import { registerLocales } from '@/shared/lib/registerLocales'
import en from './en.json'
import fr from './fr.json'

// Registers into the SAME `tasks` namespace the page uses, rather than claiming a namespace
// of its own. i18next's addResourceBundle is called with deep = true, so the page bundle and
// this one merge instead of overwriting each other. Keeping the namespace is what lets every
// relocated component keep its useTranslation('tasks') and every t() call untouched — this
// move is meant to change no behaviour, and rewriting hundreds of call sites would risk it.
registerLocales('tasks', { en, fr })
