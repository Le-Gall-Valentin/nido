import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Plus } from 'lucide-react'
import { Button } from './Button'
import { buttonClassName } from './buttonLook'

const classesOf = (element: Element) => element.getAttribute('class')?.split(/\s+/) ?? []

describe('Button', () => {
  it('renders children when not loading', () => {
    const { getByText } = render(<Button>Submit</Button>)
    expect(getByText('Submit')).toBeDefined()
  })

  it('hides spinner from screen readers when loading', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    const spinner = container.querySelector('svg')
    expect(spinner?.getAttribute('aria-hidden')).toBe('true')
  })

  it('preserves accessible label for screen readers when loading', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    const srSpan = container.querySelector('.sr-only')
    expect(srSpan?.textContent).toBe('Submit')
  })

  it('is disabled when isLoading is true', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    expect((container.querySelector('button') as HTMLButtonElement).disabled).toBe(true)
  })

  it('is the bordered secondary button unless told otherwise', () => {
    render(<Button>Cancel</Button>)
    const button = screen.getByRole('button', { name: 'Cancel' })
    expect(button.dataset.variant).toBe('secondary')
    expect(classesOf(button)).toEqual(expect.arrayContaining(['border-border', 'bg-bg-1']))
  })

  it('fills the primary button with the CTA colours, without the beige border around them', () => {
    render(<Button variant="primary">Save</Button>)
    const button = screen.getByRole('button', { name: 'Save' })
    expect(button.dataset.variant).toBe('primary')
    expect(classesOf(button)).toEqual(expect.arrayContaining(
      ['border-transparent', 'bg-[var(--btn-primary-bg)]', 'text-[var(--btn-primary-fg)]']))
    expect(classesOf(button)).not.toContain('border-border')
  })

  it('paints what cannot be undone red', () => {
    render(<Button variant="danger">Delete</Button>)
    const button = screen.getByRole('button', { name: 'Delete' })
    expect(button.dataset.variant).toBe('danger')
    expect(classesOf(button)).toEqual(expect.arrayContaining(['border-transparent', 'bg-status-red', 'text-bg-0']))
  })

  it('lets the caller\'s classes replace its own rather than race them in the stylesheet', () => {
    render(<Button className="w-full py-3.5 text-[15px]">Go</Button>)
    const classes = classesOf(screen.getByRole('button', { name: 'Go' }))
    expect(classes).toEqual(expect.arrayContaining(['w-full', 'py-3.5', 'text-[15px]']))
    expect(classes).not.toContain('py-[11px]')
    expect(classes).not.toContain('text-sm')
  })

  it('draws its icon hidden from screen readers, green on the secondary button and in the text colour on the primary one', () => {
    render(<><Button icon={Plus}>Add</Button><Button variant="primary" icon={Plus}>New</Button></>)
    const secondaryIcon = screen.getByRole('button', { name: 'Add' }).querySelector('svg')
    const primaryIcon = screen.getByRole('button', { name: 'New' }).querySelector('svg')
    expect(secondaryIcon?.getAttribute('aria-hidden')).toBe('true')
    expect(classesOf(secondaryIcon!)).toContain('text-accent')
    expect(classesOf(primaryIcon!)).not.toContain('text-accent')
  })

  it('shows the spinner in place of its icon while loading', () => {
    const { container } = render(<Button icon={Plus} isLoading>Add</Button>)
    expect(container.querySelectorAll('svg')).toHaveLength(1)
    expect(classesOf(container.querySelector('svg')!)).toContain('animate-spin')
  })

  it('comes in a small size whose label never breaks', () => {
    render(<Button size="sm">See</Button>)
    const classes = classesOf(screen.getByRole('button', { name: 'See' }))
    expect(classes).toEqual(expect.arrayContaining(['text-[12.5px]', 'whitespace-nowrap']))
    expect(classes).not.toContain('text-sm')
  })

  it('lends its look to a link through the same definition', () => {
    expect(buttonClassName({ variant: 'primary' })).toBe(
      render(<Button variant="primary">x</Button>).container.querySelector('button')!.getAttribute('class'))
  })
})
