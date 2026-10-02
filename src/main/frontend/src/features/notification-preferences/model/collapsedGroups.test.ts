import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readCollapsedGroups, writeCollapsedGroups } from './collapsedGroups'

const KEY = 'nido.notification-groups.collapsed'

describe('collapsedGroups', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.restoreAllMocks())

  it('reads nothing folded on a device that never folded anything', () => {
    expect(readCollapsedGroups()).toEqual(new Set())
  })

  it('reads back what it wrote', () => {
    writeCollapsedGroups(new Set(['space', 'tasks']))

    expect(readCollapsedGroups()).toEqual(new Set(['space', 'tasks']))
    expect(JSON.parse(localStorage.getItem(KEY) ?? '')).toEqual(['space', 'tasks'])
  })

  it('reads anything but a list of names as nothing folded', () => {
    localStorage.setItem(KEY, '{not json')
    expect(readCollapsedGroups()).toEqual(new Set())

    localStorage.setItem(KEY, '{"space":true}')
    expect(readCollapsedGroups()).toEqual(new Set())

    localStorage.setItem(KEY, '["space", 3]')
    expect(readCollapsedGroups()).toEqual(new Set(['space']))
  })

  it('storage that throws reads as nothing folded and writes nothing', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('denied') })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('denied') })

    expect(readCollapsedGroups()).toEqual(new Set())
    expect(() => writeCollapsedGroups(new Set(['space']))).not.toThrow()
  })
})
