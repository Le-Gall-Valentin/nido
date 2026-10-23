/** A series the server refused for repeating at an interval over its limit (`interval_too_large`). */
export class IntervalTooLargeError extends Error {
  constructor() { super('Interval too large'); this.name = 'IntervalTooLargeError' }
}
