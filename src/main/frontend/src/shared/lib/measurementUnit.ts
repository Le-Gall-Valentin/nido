export type MeasurementUnit =
  | 'GRAM' | 'KILOGRAM' | 'MILLILITER' | 'CENTILITER' | 'LITER'
  | 'PIECE' | 'SLICE' | 'TABLESPOON' | 'TEASPOON' | 'PINCH' | 'SACHET'

export const MEASUREMENT_UNITS: MeasurementUnit[] = [
  'GRAM', 'KILOGRAM', 'MILLILITER', 'CENTILITER', 'LITER',
  'PIECE', 'SLICE', 'TABLESPOON', 'TEASPOON', 'PINCH', 'SACHET',
]

/** Translation keys within the `common` i18n namespace — used by any consumer that can't reach into a page-private locale file (e.g. the shopping-list entity/feature). */
export const MEASUREMENT_UNIT_LABEL_KEY: Record<MeasurementUnit, string> = {
  GRAM: 'unit.GRAM',
  KILOGRAM: 'unit.KILOGRAM',
  MILLILITER: 'unit.MILLILITER',
  CENTILITER: 'unit.CENTILITER',
  LITER: 'unit.LITER',
  PIECE: 'unit.PIECE',
  SLICE: 'unit.SLICE',
  TABLESPOON: 'unit.TABLESPOON',
  TEASPOON: 'unit.TEASPOON',
  PINCH: 'unit.PINCH',
  SACHET: 'unit.SACHET',
}
