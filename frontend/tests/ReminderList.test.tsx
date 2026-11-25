import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { App } from '@/App'
import { goTo, stubApi, USER } from './apiStub'

const logan = {
  id: '44444444-4444-4444-4444-444444444444',
  nickname: 'La Logan',
  make: 'Dacia',
  model: 'Logan',
  registration: '12345-A-6',
  initialMileageKm: 40000,
  currentMileage: { mileageKm: 49500, date: '2026-05-01' },
  status: 'ACTIVE',
  createdAt: '2025-01-10T08:00:00Z',
  updatedAt: '2026-05-20T08:00:00Z',
}

const reminders = {
  dueItems: [
    {
      vehicleId: logan.id,
      vehicleNickname: 'La Logan',
      item: {
        kind: 'DEADLINE',
        status: 'OVERDUE',
        deadlineId: 'd1',
        label: 'Vignette',
        dueDate: '2026-01-31',
        remainingDays: -10,
      },
    },
  ],
  mileageReadings: [{ vehicleId: logan.id, vehicleNickname: 'La Logan', currentMileage: logan.currentMileage }],
}

describe('rappels de l’accueil', () => {
  afterEach(() => goTo('/'))

  it('liste les échéances et le relevé à faire, avec une pastille dans la navigation', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
      'GET /api/reminders': { status: 200, body: reminders },
    })
    goTo('/')
    render(<App />)

    const section = await screen.findByRole('region', { name: 'À faire' })
    expect(within(section).getByRole('link', { name: 'Vignette' })).toHaveAttribute('href', `/vehicules/${logan.id}`)
    expect(within(section).getByText('en retard de 10 jours')).toBeInTheDocument()
    expect(within(section).getByText('Relever le kilométrage de La Logan')).toBeInTheDocument()
    expect(screen.getByRole('navigation').textContent).toContain('2 rappels')

    await userEvent.click(within(section).getByRole('button', { name: 'Relever le kilométrage de La Logan' }))
    expect(await screen.findByRole('dialog', { name: 'Relever le kilométrage' })).toBeInTheDocument()
  })

  it('indique que tout est à jour', async () => {
    stubApi({
      'GET /api/me': { status: 200, body: USER },
      'GET /api/vehicles': { status: 200, body: [logan] },
      'GET /api/reminders': { status: 200, body: { dueItems: [], mileageReadings: [] } },
    })
    goTo('/')
    render(<App />)

    expect(await screen.findByText('Tout est à jour')).toBeInTheDocument()
  })
})
