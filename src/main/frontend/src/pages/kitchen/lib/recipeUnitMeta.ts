import type { MeasurementUnit } from '../model/types'

/** Order mirrors the backend enum and the mockup's unit picker. */
export const RECIPE_UNITS: MeasurementUnit[] = [
  'GRAM', 'KILOGRAM', 'MILLILITER', 'CENTILITER', 'LITER',
  'PIECE', 'SLICE', 'TABLESPOON', 'TEASPOON', 'PINCH', 'SACHET',
]

export const RECIPE_UNIT_LABEL_KEY: Record<MeasurementUnit, string> = {
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
