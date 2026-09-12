import { describe, expect, it } from 'vitest'
import { contributionLabelKeys } from './contributionLabelKeys'

describe('contributionLabelKeys', () => {
  it('speaks of a payer and contributors for an expense', () => {
    expect(contributionLabelKeys('EXPENSE')).toEqual({
      holder: 'form.payer_label',
      holderRequired: 'form.payer_required',
      parties: 'form.contributors_label',
    })
  })

  it('speaks of a receiver and beneficiaries for an income', () => {
    // Not cosmetic: on an income the holder owes the others their share, the reverse of an
    // expense. Keeping the expense wording would describe the opposite of what is recorded.
    expect(contributionLabelKeys('INCOME')).toEqual({
      holder: 'form.receiver_label',
      holderRequired: 'form.receiver_required',
      parties: 'form.beneficiaries_label',
    })
  })

  it('names keys that both locales actually define', async () => {
    // A missing key renders as the key itself, in the middle of a form, with no test failing —
    // so the two files are checked here rather than assumed.
    const fr = (await import('../locales/fr.json')).default as { form: Record<string, unknown> }
    const en = (await import('../locales/en.json')).default as { form: Record<string, unknown> }

    for (const type of ['EXPENSE', 'INCOME'] as const) {
      for (const key of Object.values(contributionLabelKeys(type))) {
        const leaf = key.replace('form.', '')
        expect(fr.form[leaf], `fr: ${key}`).toBeTypeOf('string')
        expect(en.form[leaf], `en: ${key}`).toBeTypeOf('string')
      }
    }
  })
})
