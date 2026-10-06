/** Where a one-time link stands once its page asked the server: usable, refused, or not asked yet. */
export type LinkCheckState = 'checking' | 'valid' | 'invalid' | 'unavailable'
