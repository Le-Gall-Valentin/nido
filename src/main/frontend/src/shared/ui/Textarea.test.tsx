import { render, screen } from '@testing-library/react'
import { Textarea } from './Textarea'
import { describe, it, expect } from 'vitest'

describe('Textarea', () => {
  it('renders with an accessible label', () => {
    render(<Textarea label="Description" id="description" />)
    expect(screen.getByLabelText('Description')).not.toBeNull()
  })

  it('passes through textarea attributes', () => {
    render(<Textarea label="Notes" id="notes" placeholder="Write something" rows={4} />)
    const textarea = screen.getByLabelText('Notes') as HTMLTextAreaElement
    expect(textarea.placeholder).toBe('Write something')
    expect(textarea.rows).toBe(4)
  })

  it('is vertically resizable', () => {
    render(<Textarea label="Notes" id="notes" />)
    expect(screen.getByLabelText('Notes').className).toContain('resize-y')
  })
})
